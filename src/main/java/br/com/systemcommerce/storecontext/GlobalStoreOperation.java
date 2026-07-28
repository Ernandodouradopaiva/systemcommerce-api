package br.com.systemcommerce.storecontext;

/** Endpoint administrativo/global que não exige loja ativa. */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface GlobalStoreOperation {}
