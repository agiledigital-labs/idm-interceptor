package au.com.agiledigital.idm;

import au.com.agiledigital.idm.interceptor.RequestHandlerInterceptor;

public interface SharedInterceptorStateService {

	void registerInterceptor(String factoryPid, RequestHandlerInterceptor interceptor);

	void unregisterInterceptor(String factoryPid, RequestHandlerInterceptor interceptor);

}