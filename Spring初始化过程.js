Spring的初始化过程
这边的初始化过程将使用如下的代码作为入口开始说明com.chenss.test.Test

1. 创建注解配置上下文
AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(); {
    该类的初始化过程会首先调用父类的初始化过程


    在初始化过程中，主要是初始化了注解bean定义读取器AnnotatedBeanDefinitionReader和初始化了类路径bean定义扫描器ClassPathBeanDefinitionScanner。

    1.1 注解bean定义读取器，主要用于
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
2. 注册配置文件
annotationConfigApplicationContext.register(Appconfig.class);
3. 注册完成后，需要刷新操作，用于生效
annotationConfigApplicationContext.refresh();
4. 获取初始化完成的Bean
UserDao userDao = (UserDao) annotationConfigApplicationContext.getBean("userDao");


org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List<org.springframework.beans.factory.config.BeanFactoryPostProcessor>)
首先调用BeanDefinitionRegistryPostProcessor，这个是我们自己注册或预先注册的类型，存储在应用程序上下文的this.beanFactoryPostProcessors，调用其postProcessBeanDefinitionRegistry方法

