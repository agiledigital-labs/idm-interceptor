package au.com.agiledigital.idm;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;

import au.com.agiledigital.idm.interceptor.RequestHandlerInterceptor;

@Component(scope = ServiceScope.SINGLETON)
public class SharedInterceptorState implements SharedInterceptorStateService {
	
	private ConcurrentHashMap<String, RequestHandlerInterceptor> interceptorMap = new ConcurrentHashMap<>();
	
	@Override
	public void registerInterceptor(String factoryPid, RequestHandlerInterceptor interceptor) {
		interceptorMap.put(factoryPid, interceptor);
	}
	
	@Override
	public void unregisterInterceptor(String factoryPid, RequestHandlerInterceptor interceptor) {
		interceptorMap.remove(factoryPid);
	}
}
