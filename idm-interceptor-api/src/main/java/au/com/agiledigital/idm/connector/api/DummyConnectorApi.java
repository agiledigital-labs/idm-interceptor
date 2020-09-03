package au.com.agiledigital.idm.connector.api;

import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

public abstract class DummyConnectorApi implements PoolableConnector, CreateOp, DeleteOp, SearchOp<String>, UpdateOp, TestOp {

}
