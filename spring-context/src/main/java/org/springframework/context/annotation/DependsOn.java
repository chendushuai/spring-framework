/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前bean所依赖的bean。
 * 任何指定的bean都保证在此bean之前由容器创建。
 * 当一个bean不是通过属性或构造函数参数显式地依赖于另一个bean，而是依赖于另一个bean的初始化的副作用时，很少使用。
 *
 * <p>依赖项声明depends-on既可以指定初始化时间依赖项，也可以指定(在只有单例bean的情况下)相应的销毁时间依赖项。
 * 与给定bean定义依赖关系的依赖bean首先被销毁，先于给定bean本身被销毁。因此，依赖声明也可以控制关机顺序。
 *
 * <p>可以直接或间接地用{@link org.springframework.stereotype.Component}注解或使用{@link Bean}注释的方法在任何类上使用。
 *
 * <p>除非使用组件扫描，否则在类级别上使用{@link DependsOn}没有效果。
 * 如果一个{@link DependsOn}注释的类是通过XML声明的，那么{@link DependsOn}注释元数据将被忽略，
 * 而{@code <bean depends-on="..."/>}反而更重要。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {

	String[] value() default {};

}
