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

/**
 * Called when there is an error with an Action
 */
public class ActionException extends Exception {

    private static final long serialVersionUID = 5467945798187403789L;

    /**
     * Creates a new ActionException
     */
    public ActionException() {
        super();
    }

    /**
     * Creates a new ActionException with an error message
     * @param message Error message
     */
    public ActionException(final String message) {
        super(message);
    }

    /**
     * Creates a new ActionException with an error
     * @param throwable Error
     */
    public ActionException(final Throwable throwable) {
        super(throwable);
    }
}
