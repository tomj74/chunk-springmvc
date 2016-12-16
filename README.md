# chunk-springmvc
Chunk Templates for Spring MVC

Facilitates swapping in the Chunk Template engine for Spring MVC projects
in place of jsp or another template engine.

### Quick start:

Add the following to your dispatcher-servlet.xml:
```
  <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="viewClass" value="com.x5.template.spring.ChunkTemplateView"/>
    <property name="prefix" value="/WEB-INF/chunk/"/>
    <property name="suffix" value=".chtml"/>
    <property name="requestContextAttribute" value="rc"/>
  </bean>

  <bean id="chunkTemplatesConfig" class="java.util.HashMap" scope="prototype">
    <constructor-arg>
      <map key-type="java.lang.String" value-type="java.lang.String">
        <entry key="default_extension" value="chtml" />
        <entry key="cache_minutes" value="0" />
        <entry key="layers" value="" />
        <entry key="theme_path" value="" />
        <entry key="hide_errors" value="FALSE" />
        <entry key="error_log" value="" />
        <entry key="encoding" value="UTF-8" />
        <entry key="locale" value="" />
        <entry key="filters" value="" />
      </map>
    </constructor-arg>
  </bean>
```

Complete project setup guide here:
http://www.x5software.com/chunk/wiki/Spring_MVC

### Notes

MVC Framework localized messages (defined in messages.properties etc)
are available via a custom tag command:
```
{% messages.msg.name %}
```

Messages can be parameterized like so: ``{% messages.msg.name(`$a`,`$b`) %}``

A special request context tag is available, usually {$rc} but the name is
configurable.

The following request context values are available:
```
 {$rc.uri}
 {$rc.context_path}
 {$rc.servlet_path}
 {$rc.scheme}
 {$rc.method}
 {$rc.server_name}
 {$rc.remote_addr}
 {$rc.remote_host}
 {$rc.remote_user}
```

### Using bean getters in a template

When exposing beans (or POJO member variables) in a model, keep in mind that
Chunk always converts ``camelCase`` to ``snake_case``.

So, ``x.getFavoriteColor()`` will be available in the template as
``{% $x.favorite_color %}`` -- and ``x.isBigAndHairy()`` must be checked
like so:
```
{% if $x.big_and_hairy %}...{% endif %}
```
