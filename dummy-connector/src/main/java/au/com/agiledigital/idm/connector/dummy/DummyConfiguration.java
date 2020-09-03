package au.com.agiledigital.idm.connector.dummy;

import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class DummyConfiguration extends AbstractConfiguration {

	private String dummyConnectorFactoryPid;

	public void validate() {
	}

	@ConfigurationProperty
	public String getDummyConnectorFactoryPid() {
		return dummyConnectorFactoryPid;
	}

	public void setDummyConnectorFactoryPid(String dummyConnectorFactoryPid) {
		this.dummyConnectorFactoryPid = dummyConnectorFactoryPid;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DummyConfiguration other = (DummyConfiguration) obj;
		if (dummyConnectorFactoryPid == null) {
			if (other.dummyConnectorFactoryPid != null)
				return false;
		} else if (!dummyConnectorFactoryPid.equals(other.dummyConnectorFactoryPid))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dummyConnectorFactoryPid == null) ? 0 : dummyConnectorFactoryPid.hashCode());
		return result;
	}
}
