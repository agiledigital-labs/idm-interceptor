package au.com.agiledigital.idm;

public interface SharedInterceptorStateService {

	void registerInterceptor(String factoryPid, RequestHandlerInterceptor interceptor);

	void unregisterInterceptor(String factoryPid, RequestHandlerInterceptor interceptor);

}