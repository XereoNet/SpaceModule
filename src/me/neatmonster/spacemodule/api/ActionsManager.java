/*
 * This file is part of SpaceModule (http://spacebukkit.xereo.net/).
 *
 * SpaceModule is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceBukkit is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacemodule.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Manages actions and calls them
 */
public class ActionsManager {
    protected Map<String, Method> actions = new HashMap<String, Method>();
    protected Map<Class<?>, ActionHandler> handlers = new HashMap<Class<?>, ActionHandler>();

    /**
     * Casts an object
     * @param object Object to cast
     * @param current Current class
     * @param expected Expected class
     * @return The casted object, null if couldn't be casted
     */
    public Object cast(final Object object, final Class<?> current, final Class<?> expected) {
        try {
            if (expected == Object[].class)
                if (current.isArray())
                    return Arrays.asList(object).toArray();
                else if (current.isAssignableFrom(List.class))
                    return ((List<?>) object).toArray();
                else
                    return new Object[] {object};
            final String string = object.toString();
            if (expected == String.class)
                return string;

            else if (expected == Character.class || expected == char.class)
                return string.charAt(0);

            else if (expected == Byte.class || expected == byte.class)
                return Byte.parseByte(string);

            else if (expected == Short.class || expected == short.class)
                return Short.parseShort(string);

            else if (expected == Integer.class || expected == int.class)
                return Integer.parseInt(string);

            else if (expected == Long.class || expected == long.class)
                return Long.parseLong(string);

            else if (expected == Float.class || expected == float.class)
                return Float.parseFloat(string);

            else if (expected == Double.class || expected == double.class)
                return Double.parseDouble(string);

            else if (expected == Boolean.class || expected == boolean.class)
                return Boolean.parseBoolean(string);

            return null;

        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Checks if an action is defined by an alias
     * @param alias Alias to check
     * @return If an action is defined
     */
    public boolean contains(final String alias) {
        return actions.containsKey(alias.toLowerCase());
    }

    /**
     * Executes an action
     * @param alias Action name
     * @param arguments Arguments to execute the action with
     * @return Result of the action
     * @throws InvalidArgumentsException If an action cannot be found
     * @throws UnhandledActionException If an action is unhandled
     */
    public Object execute(final String alias, final Object... arguments) throws InvalidArgumentsException,
            UnhandledActionException {
        final Method method = actions.get(alias.toLowerCase());
        if (method == null)
            throw new UnhandledActionException();

        if(method.getParameterTypes().length != arguments.length)
            throw new InvalidArgumentsException(method.getParameterTypes().length + " arguments expected, not "
                    + arguments.length + " for method " + method.getName() + ".");

        for (int a = 0; a < method.getParameterTypes().length; a++) {
            if(arguments[a] == null || arguments[a].getClass() == null || arguments[a].getClass().getName() == null) //XXX: This is ugly and needs to burn
                throw new InvalidArgumentsException("null parameters are not allowed for method " + method.getName() + ".");
            if (!arguments[a].getClass().getName().equals(method.getParameterTypes()[a].getName())) {
                final Object casted = cast(arguments[a], arguments[a].getClass(), method.getParameterTypes()[a]);
                if (casted == null)
                    throw new InvalidArgumentsException(method.getParameterTypes()[a].getSimpleName() + " ("
                            + method.getParameterTypes()[a].getName() + ") expected, not "
                            + arguments[a].getClass().getSimpleName() + " (" + arguments[a].getClass().getName()
                            + ") for method " + method.getName() + ".");
                else
                    arguments[a] = casted;
            }
        }
        return invoke(method, arguments);
    }

    /**
     * Involkes a method
     * @param method Method to involke
     * @param arguments Arguments to involke the method with
     * @return The result of the method
     */
    protected Object invoke(final Method method, final Object... arguments) {
        try {
            ActionHandler handler = handlers.get(method.getDeclaringClass());

            if(handler == null) {
                handler = (ActionHandler)method.getDeclaringClass().newInstance();
                handlers.put(method.getDeclaringClass(), handler);
            }
            return method.invoke(handler, arguments);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();

        } catch (IllegalAccessException e) {
            e.printStackTrace();

        } catch (InvocationTargetException e) {
            e.printStackTrace();

        } catch (SecurityException e) {
            e.printStackTrace();

        } catch (InstantiationException e) {
            e.printStackTrace();

        } catch(ClassCastException e) {
            e.printStackTrace();

        }
        return null;
    }

    /**
     * Checks if an action is schedulable
     * @param alias Action name
     * @return If an action is schedulable
     * @throws UnhandledActionException If the action is not defined
     */
    public boolean isSchedulable(final String alias) throws UnhandledActionException {
        final Method method = actions.get(alias.toLowerCase());
        if (method == null)
            throw new UnhandledActionException();
        final Action action = method.getAnnotation(Action.class);
        return action.schedulable();
    }

    /**
     * Registeres a class and sets up it's actions
     * @param class_ Class to register
     */
    public void register(final Class<?> class_) {
        for (final Method method : class_.getMethods()) {
            if (!method.isAnnotationPresent(Action.class))
                continue;
            final Action action = method.getAnnotation(Action.class);
            for (final String alias : action.aliases())
                actions.put(alias.toLowerCase(), method);
        }
    }
}
