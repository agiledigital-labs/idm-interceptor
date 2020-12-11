import openidmLib from '../util/openidm-lib';
import each from 'jest-each';
import idm from 'lib/idm';
import { equals, startsWith, trueVal } from '@agiledigital/idm-ts-types/lib/query-filter';
import * as ldapAccounts from '../data/ldapAccounts';

export const clearAll = () => {
    each`
        name                  | resource
        ${"Users"}            | ${openidmLib.managed.users}
        ${"Links"}            | ${openidmLib.links}
    `.test('$name should be reset to be empty', async ( { resource} ) => {
        await resource.deleteAll();
        const result =  await resource.getAll().expect(200)
        expect(result.body.resultCount).toBe(0);
    }, 15000)

    test('Clear Dummy Data and Connector Events', async() => {
        const status = await openidmLib.connectorEvents.clearAll().then(res => res.status);
        expect(status).toBe(200);
    }, 1000);
};

describe('No data in LDAP, creates are performed', () => {
    clearAll();
    test('reconcile csv', async () => {
        const status = await openidmLib.reconcile.csvToManagedUser().then(res => res.body);
        expect(status.state).toBe('SUCCESS');
    }, 3000);
    test('created in LDAP', async () => {
        // There were 2 create events
        const ldapCreateEvents = await idm.system.connectoreventsCreate.query({filter: equals("objectType", "system/adeldap/account")});
        expect(ldapCreateEvents.resultCount).toBe(2);

        // And no update events
        const ldapUpdateEvents = await idm.system.connectoreventsUpdate.query({filter: equals("objectType", "system/adeldap/account")});
        expect(ldapUpdateEvents.resultCount).toBe(0);

        const adeLdapQuery = await idm.system.adeldapAccount.query({filter: trueVal()});

        adeLdapQuery.result.forEach(acc => {
            expect(acc).toMatchSnapshot({
                _id: expect.any(String)
            }, `Expected LDAP Created Account [${acc.uid}]`)
        });
    }, 5000);
});

describe('Existing data in LDAP, updates are performed', () => {
    clearAll();
    each`
        name                    | values                                  | resource
        ${"LDAP accounts"}      | ${ldapAccounts.initialValues}           | ${openidmLib.system.ldap.accounts}
    `.test('loading $name data', async ( { values, resource }) => {
        const result = await resource.createAll(values);
        const failures = result.filter(item => item.status !== 201);
        expect(failures.length).toBe(0);
    });
    test('check for create events', async () => {
        // There were 2 create events
        const ldapCreateEvents = await idm.system.connectoreventsCreate.query({filter: equals("objectType", "system/adeldap/account")});
        expect(ldapCreateEvents.resultCount).toBe(2);
    
        // Clear the events before reconciling
        const statusClearEvents = await openidmLib.connectorEvents.clearEvents().then(res => res.status);
        expect(statusClearEvents).toBe(200);
    }, 3000);
    test('reconcile csv', async () => {
        const status = await openidmLib.reconcile.csvToManagedUser().then(res => res.body);
        expect(status.state).toBe('SUCCESS');
    }, 3000);
    test('updated in LDAP', async () => {
        // There were 0 create events
        const ldapCreateEvents = await idm.system.connectoreventsCreate.query({filter: equals("objectType", "system/adeldap/account")});
        expect(ldapCreateEvents.resultCount).toBe(0);

        // And 2 update events
        const ldapUpdateEvents = await idm.system.connectoreventsUpdate.query({filter: startsWith("objectType", "system/adeldap/account")});
        expect(ldapUpdateEvents.resultCount).toBe(2);

        const adeLdapQuery = await idm.system.adeldapAccount.query({filter: trueVal()});

        adeLdapQuery.result.forEach(acc => {
            expect(acc).toMatchSnapshot({
                _id: expect.any(String)
            }, `Expected LDAP Updated Account [${acc.uid}]`)
        });
    }, 5000);
});