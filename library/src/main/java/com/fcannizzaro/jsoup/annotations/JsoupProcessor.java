package com.fcannizzaro.jsoup.annotations;

import com.fcannizzaro.jsoup.annotations.interfaces.Attr;
import com.fcannizzaro.jsoup.annotations.interfaces.Child;
import com.fcannizzaro.jsoup.annotations.interfaces.ForEach;
import com.fcannizzaro.jsoup.annotations.interfaces.Items;
import com.fcannizzaro.jsoup.annotations.interfaces.Selector;
import com.fcannizzaro.jsoup.annotations.interfaces.Text;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Francesco Cannizzaro (fcannizzaro)
 */

public class JsoupProcessor {

    /**
     * Extract first element according to a query
     */
    private static Element element(Element container, String query) {
        return container.select(query).first();
    }

    /**
     * Bind a Jsoup element to a class
     *
     * @param container dom element
     * @param clazz     object class
     * @return object instance
     */
    public static <T> T from(Element container, Class<T> clazz) {

        try {

            T instance = clazz.newInstance();

            for (Field field : clazz.getFields()) {

                Selector selector = field.getAnnotation(Selector.class);
                Text text = field.getAnnotation(Text.class);
                Child child = field.getAnnotation(Child.class);
                Items items = field.getAnnotation(Items.class);
                Attr attr = field.getAnnotation(Attr.class);

                Object value = null;

                if (items != null) {
                    ParameterizedType type = (ParameterizedType) field.getGenericType();
                    Class<?> cz = (Class<?>) type.getActualTypeArguments()[0];
                    value = fromList(container, cz);
                }

                if (child != null) {

                    Class cz = field.getType();
                    Selector sel = (Selector) cz.getAnnotation(Selector.class);

                    if (sel != null) {
                        value = from(element(container, sel.value()), cz);
                    }

                }

                if (selector != null) {
                    value = element(container, selector.value());
                }

                if (text != null) {

                    Element el = element(container, text.value());

                    if (el != null) {
                        value = el.text();
                    }

                }

                if (attr != null) {

                    Element el = element(container, attr.value());

                    if (el != null) {
                        value = el.attr(attr.attr());
                    }

                }

                if (value != null) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }

            }

            for (Method method : clazz.getMethods()) {

                ForEach forEach = method.getAnnotation(ForEach.class);

                if (forEach != null) {

                    method.setAccessible(true);

                    Elements elements = container.select(forEach.value());

                    for (int i = 0; i < elements.size(); i++) {

                        Element element = elements.get(i);

                        if (method.getParameterTypes().length > 1) {
                            method.invoke(instance, element, i);
                            continue;
                        }

                        method.invoke(instance, element);

                    }

                }

            }

            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Bind multiple object. (Internally use from)
     */
    public static <T> List<T> fromList(Element container, Class<T> clazz) {

        ArrayList<T> items = new ArrayList<>();

        Selector selector = clazz.getAnnotation(Selector.class);

        if (selector != null) {

            Elements elements = container.select(selector.value());

            for (Element element : elements) {
                items.add(from(element, clazz));
            }

        }

        return items;

    }

}