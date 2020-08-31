package au.com.agiledigital.idm.connector;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

public class DummyConnector implements CreateOp, DeleteOp, SearchOp<String>, UpdateOp, SchemaOp {

	private ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>> data;

	private String factoryPid;

	private ConcurrentMap<Uid, Set<Attribute>> getObjectClassMap(ObjectClass objectClass) {
		ConcurrentMap<Uid, Set<Attribute>> classMap = data.computeIfAbsent(objectClass,
				key -> new ConcurrentHashMap<>());

		return classMap;
	}

	public DummyConnector(String factoryPid,
			Optional<ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>>> data) {
		this.factoryPid = factoryPid;
		this.data = data.orElse(new ConcurrentHashMap<>());
	}

	@Override
	public Schema schema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);
		
		if (!objectClassMap.containsKey(uid)) {
			throw new UnknownUidException(String.format("%s: %s: Uid %s doesn't exist.", this.factoryPid,
					objClass.toString(), uid.getUidValue()));
		}
		return uid;
	}

	@Override
	public FilterTranslator<String> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
		return new DummyFilterTranslator();
	}

	@Override
	public void executeQuery(ObjectClass arg0, String arg1, ResultsHandler arg2, OperationOptions arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);
		objectClassMap.remove(uid);
	}

	@Override
	public Uid create(ObjectClass objClass, Set<Attribute> attrs, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);

		// Find a Uid or generate one.
		Uid uid = attrs.stream().filter(attr -> attr.getName().equals(Uid.NAME))
				.map(attr -> new Uid(AttributeUtil.getAsStringValue(attr))).findFirst()
				.orElse(new Uid(UUID.randomUUID().toString()));

		Set<Attribute> existingAttrs = objectClassMap.putIfAbsent(uid, attrs);

		if (attrs != existingAttrs) {
			throw new AlreadyExistsException(String.format("%s: %s: %s already exists.", this.factoryPid,
					objClass.toString(), uid.getUidValue()));
		}

		return uid;
	}
}
