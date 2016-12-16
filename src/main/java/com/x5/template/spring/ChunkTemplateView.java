package com.x5.template.spring;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.io.Resource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.InternalResourceView;

import com.x5.template.Chunk;
import com.x5.template.Snippet;
import com.x5.template.Theme;
import com.x5.template.ThemeConfig;
import com.x5.template.filters.FilterArgs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * ChunkTemplateView facilitates swapping in the Chunk Template engine
 * for Spring MVC projects.
 *
 * Messages are available via {% messages.msg.name %}
 * Messages can be parameterized like so: {% messages.msg.name(`$a`,`$b`) %}
 *
 * A special request context tag is available, usually {$rc} but the name is
 * configurable.
 *
 * The following request context values are available:
 *  {$rc.uri}
 *  {$rc.context_path}
 *  {$rc.servlet_path}
 *  {$rc.scheme}
 *  {$rc.method}
 *  {$rc.server_name}
 *  {$rc.remote_addr}
 *  {$rc.remote_host}
 *  {$rc.remote_user}
 *
 * When exposing beans in a model, keep in mind that Chunk always converts
 * camelCase to snake_case. So, x.getFavoriteColor() will be available in the
 * template as {% $x.favorite_color %} and x.isBigAndHairy() can be checked
 * like so: {% if $x.big_and_hairy %}...{% endif %}
 */
public class ChunkTemplateView extends InternalResourceView
{
    private static Theme theme = null;

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
    throws Exception
    {
        Resource templateFile = getApplicationContext().getResource(getUrl());

        String rcKey = getRequestContextAttribute();
        RequestContext rc = (RequestContext)model.get(rcKey);

        Theme theme = getTheme(templateFile.getFile().getParent());
        Chunk chunk = theme.makeChunk(getBeanName());
        chunk.setLocale(rc.getLocale());
        chunk.setMultiple(model);
        chunk.set(rcKey, mapifyRequestContext(rc, request));

        PrintWriter writer = response.getWriter();
        chunk.render(writer);
        writer.flush();
        writer.close();
    }

    private Map<String,String> mapifyRequestContext(RequestContext rc, HttpServletRequest request)
    {
        Map<String,String> rcMap = new HashMap<String,String>();

        // expose some potentially useful info to the template via the {$rc} tag
        rcMap.put("uri", rc.getRequestUri());
        rcMap.put("context_path", rc.getContextPath());
        rcMap.put("servlet_path", rc.getPathToServlet());
        rcMap.put("scheme", request.getScheme());
        rcMap.put("method", request.getMethod());
        rcMap.put("server_name", request.getServerName());
        rcMap.put("remote_addr", request.getRemoteAddr());
        rcMap.put("remote_host", request.getRemoteHost());
        rcMap.put("remote_user", request.getRemoteUser());

        return rcMap;
    }

    private Theme getTheme(String path)
    {
        if (theme == null) {
            Map<String,String> params = new HashMap<String,String>();
            // If no theme path (for include/exec references) is specified
            // in the config, default to the path of the invoked template file.
            params.put(ThemeConfig.THEME_PATH, path);

            Map<String,String> configParams = getConfigParams();
            if (configParams != null) {
                for (String key : configParams.keySet()) {
                    String paramName = key;
                    String paramValue = configParams.get(key);
                    // blank values are considered not-provided
                    if (paramValue != null && paramValue.trim().length() > 0) {
                        params.put(paramName, paramValue);
                    }
                }
            }
            ThemeConfig config = new ThemeConfig(params);
            theme = new Theme(config);
            theme.addProtocol( new MessagesProtocol() );
        }

        return theme;
    }

    @SuppressWarnings("unchecked")
    private Map<String,String> getConfigParams()
    {
        try {
            Object config = getApplicationContext().getBean("chunkTemplatesConfig");
            return (Map<String,String>)config;
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    class MessagesProtocol implements com.x5.template.ContentSource
    {
        public MessagesProtocol() {}

        public String fetch(String resource)
        {
            FilterArgs argParser = new FilterArgs(resource);
            Object[] args = argsAsObjArray(argParser);
            String messageRef = argParser.getFilterName();
            Locale locale = LocaleContextHolder.getLocale();

            return getApplicationContext().getMessage(messageRef, args, locale);
        }

        private Object[] argsAsObjArray(FilterArgs parser)
        {
            String[] args = parser.getFilterArgs();
            if (args == null || args.length == 0) {
                return new Object[0];
            }

            Object[] oArgs = new Object[args.length];
            for (int i=0; i<args.length; i++) {
                oArgs[i] = args[i];
            }
            return oArgs;
        }

        public String getProtocol()
        {
            return "messages";
        }

        public Snippet getSnippet(String resource)
        {
            return Snippet.getSnippet(fetch(resource));
        }

        public boolean provides(String resource)
        {
            return fetch(resource) != null;
        }
    }
}