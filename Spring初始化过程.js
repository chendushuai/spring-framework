Spring的初始化过程
这边的初始化过程将使用如下的代码作为入口开始说明

1. 创建注解配置上下文
AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(); {

}
2. 注册配置文件
annotationConfigApplicationContext.register(Appconfig.class);
3. 注册完成后，需要刷新操作，用于生效
annotationConfigApplicationContext.refresh();
4. 获取初始化完成的Bean
UserDao userDao = (UserDao) annotationConfigApplicationContext.getBean("userDao");