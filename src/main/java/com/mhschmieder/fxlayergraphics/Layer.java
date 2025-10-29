/*
 * MIT License
 *
 * Copyright (c) 2020, 2025 Mark Schmieder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This file is part of the FxLayerGraphics Library
 *
 * You should have received a copy of the MIT License along with the
 * FxLayerGraphics Library. If not, see <https://opensource.org/licenses/MIT>.
 *
 * Project: https://github.com/mhschmieder/fxlayergraphics
 */
package com.mhschmieder.fxlayergraphics;

import com.mhschmieder.jcommons.lang.LabeledObject;
import javafx.scene.paint.Color;

/**
 * Basic Layer implementation for common settings that cut across most
 * applications that expose some form of layer management. Use this version for
 * most purposes; there is an observable LayerProperties class in the controls
 * library for Layer Management, to be used for binding to related GUI controls.
 */
public class Layer implements Comparable< Layer >, LabeledObject {

    private String layerName;
    private Color layerColor;
    private boolean layerActive;
    private boolean layerVisible;
    private boolean layerLocked;

    public Layer( final String pLayerName,
                  final Color pLayerColor,
                  final boolean pLayerActive,
                  final boolean pLayerVisible,
                  final boolean pLayerLocked ) {
        layerName = pLayerName;
        layerColor = pLayerColor;
        layerActive = pLayerActive;
        layerVisible = pLayerVisible;
        layerLocked = pLayerLocked;
    }

    // NOTE: This is implemented strictly for sorting by Layer Name.
    @Override
    public int compareTo( final Layer otherLayerProperties ) {
        // If this Layer is the Default Layer, it is "less than" the other.
        final String thisLayerName = getLayerName();
        if ( LayerManager.DEFAULT_LAYER_NAME.equals( thisLayerName ) ) {
            return -1;
        }

        // If the other Layer is the Default Layer, it is "more than" this.
        final String otherLayerName = otherLayerProperties.getLayerName();
        if ( LayerManager.DEFAULT_LAYER_NAME.equals( otherLayerName ) ) {
            return 1;
        }

        return thisLayerName.compareTo( otherLayerName );
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName( final String pLayerName ) {
        layerName = pLayerName;
    }

    public Color getLayerColor() {
        return layerColor;
    }

    public void setLayerColor( final Color pLayerColor ) {
        layerColor = pLayerColor;
    }

    public boolean isLayerActive() {
        return layerActive;
    }

    public void setLayerActive( final boolean pLayerActive ) {
        layerActive = pLayerActive;
    }

    public boolean isLayerVisible() {
        return layerVisible;
    }

    public void setLayerVisible( final boolean pLayerVisible ) {
        layerVisible = pLayerVisible;
    }

    public boolean isLayerLocked() {
        return layerLocked;
    }

    public void setLayerLocked( final boolean pLayerLocked ) {
        layerLocked = pLayerLocked;
    }

    @Override
    public String getLabel() {
        return getLayerName();
    }

    @Override
    public void setLabel( String label ) {
        setLayerName( label );
    }
}
