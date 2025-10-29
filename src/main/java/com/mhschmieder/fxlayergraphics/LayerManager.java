/*
 * MIT License
 *
 * Copyright (c) 2020, 2025, Mark Schmieder. All rights reserved.
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

import com.mhschmieder.jcommons.lang.LabeledObjectManager;
import javafx.scene.paint.Color;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to manage non-observable layers.
 */
public final class LayerManager {

    // Declare default values for all the Layer Properties.
    public static final String  LAYER_NAME_DEFAULT    = "Layer";    //$NON-NLS-1$
    public static final boolean LAYER_STATUS_DEFAULT  = false;
    public static final boolean LAYER_DISPLAY_DEFAULT = true;
    public static final boolean LAYER_LOCK_DEFAULT    = false;
    public static final Color   LAYER_COLOR_DEFAULT   = Color.BLACK;

    // Declare a temporary Layer name, for features such as Cut/Copy/paste.
    public static final String  TEMP_LAYER_NAME       = "temp";     //$NON-NLS-1$

    // Declare the "various" Layer Name (invariant).
    public static final String  VARIOUS_LAYER_NAME    = "various";  //$NON-NLS-1$

    // Declare the name of the Default Layer (invariant).
    public static final String  DEFAULT_LAYER_NAME    = "Layer 0";  //$NON-NLS-1$

    // Declare the enforced default index of 0 for the Default Layer.
    public static final int     DEFAULT_LAYER_INDEX   = 0;

    public static void addLayer( final List< Layer > layerCollection,
                                 final Layer layerCandidate,
                                 final NumberFormat uniquefierNumberFormat ) {
        // Prevent exceptions by filtering for null objects.
        if ( layerCandidate == null ) {
            return;
        }

        // Try to use the cached Layer Name if it exists and is non-empty;
        // otherwise apply the uniquefied Layer Name Default.
        final String labelToExclude = null;
        String layerCandidateName = layerCandidate.getLayerName();
        if ( ( layerCandidateName == null ) || layerCandidateName.trim().isEmpty() ) {
            // Recursively search for (and enforce) name-uniqueness of the
            // default Layer Name, but always use a uniquefier appendix so that
            // none of them are unadorned (even the first).
            layerCandidateName = LAYER_NAME_DEFAULT;
            final int uniquefierNumber = 1;
            layerCandidateName = LabeledObjectManager.getUniqueLabel(
                    layerCollection,
                    layerCandidateName,
                    labelToExclude,
                    uniquefierNumber,
                    uniquefierNumberFormat );
        }
        else {
            // Recursively search for (and enforce) name-uniqueness of the
            // Layer candidate, leaving unadorned if possible.
            layerCandidateName = LabeledObjectManager.getUniqueLabel( 
                    layerCollection,
                    layerCandidateName,
                    labelToExclude,
                    uniquefierNumberFormat );
        }

        // Reset the Layer Name candidate, in case it changed.
        layerCandidate.setLayerName( layerCandidateName );

        // Now that we have dealt with name-uniqueness, add the Layer candidate
        // to the Layer Collection.
        layerCollection.add( layerCandidate );
    }

    // Add a Layer clone to the collection (guaranteed to be unique).
    public static Layer addLayerClone( final List< Layer > layerCollection,
                                       final int insertLayerIndex ) {
        if ( layerCollection.isEmpty() ) {
            return null;
        }

        final int referenceLayerIndex = insertLayerIndex - 1;
        final Layer newLayer = getLayerClone( referenceLayerIndex, layerCollection );
        layerCollection.add( insertLayerIndex, newLayer );

        return newLayer;
    }

    // Add a Layer candidate to the collection if not already present.
    public static void addLayerIfUnique( final List< Layer > layerCollection,
                                         final Layer layerCandidate ) {
        if ( !hasLayer( layerCollection, layerCandidate ) ) {
            // Add the new Layer to the end of the Layer Collection.
            layerCollection.add( layerCandidate );
        }
    }

    // Enforce the Active Layer Policy, which is that only one Layer can be
    // Active at a time. Default to the Default Layer if none are Active.
    private static Layer enforceActiveLayerPolicy(
            final List< Layer > layerCollection,
            final int currentLayerIndex,
            final boolean exemptDefaultLayer ) {
        // If the Layer we're intending to make Active is Hidden, make Inactive.
        // NOTE: We conditionally exempt the Default Layer (if invoked from a
        // high-level dirty flag callback), to avoid all Layers being Inactive.
        if ( !exemptDefaultLayer || ( currentLayerIndex != DEFAULT_LAYER_INDEX ) ) {
            // If setting Active Status is not allowed (due to the Selected
            // Layer being Hidden) -- resulting in no consequent change from the
            // pre-toggle state -- re-clear the Selected Layer and exit, as
            // otherwise we miss some view-syncing due to no "changed" event.
            if ( isLayerHidden( layerCollection, currentLayerIndex ) ) {
                final Layer currentLayer = getLayer(
                        layerCollection, currentLayerIndex );
                if ( currentLayer.isLayerActive() ) {
                    currentLayer.setLayerActive( false );
                }

                final Layer activeLayer = getActiveLayer( layerCollection );
                return activeLayer;
            }
        }

        // In order to avoid consuming a desired positive setting of a new
        // Active Layer when a higher row is inactivated, we must first set the
        // new Active Layer and then inactivate the rest.
        final Layer activeLayer = setActiveLayer( layerCollection, currentLayerIndex );

        for ( int layerIndex = 0, numberOfLayers = layerCollection
                .size(); layerIndex < numberOfLayers; layerIndex++ ) {
            final Layer layer = layerCollection.get( layerIndex );
            if ( layerIndex != currentLayerIndex ) {
                if ( layer.isLayerActive() ) {
                    layer.setLayerActive( false );
                }
            }
        }

        return activeLayer;
    }

    // Enforce the Active Layer Policy, which is that only one Layer can be
    // Active at a time. Default to the Default Layer if none are Active.
    public static Layer enforceActiveLayerPolicy(
            final List< Layer > layerCollection,
            final String currentLayerName,
            final boolean exemptDefaultLayer ) {
        final int activeLayerIndex
                = getLayerIndex( layerCollection, currentLayerName );

        return enforceActiveLayerPolicy(
                layerCollection,
                activeLayerIndex,
                exemptDefaultLayer );
    }

    // Enforce the Hidden Layer Policy, which is only that a Hidden Layer cannot
    // be made Active, and to default to the Default Layer if the current Layer
    // is Active and we are trying to set it to Hidden.
    public static void enforceHiddenLayerPolicy(
            final List< Layer > layerCollection,
            final int currentLayerIndex,
            final boolean currentLayerVisible ) {
        // Always cache the new Hidden status as that is always accepted.
        final Layer layer = getLayer( layerCollection, currentLayerIndex );
        if ( currentLayerVisible != layer.isLayerVisible() ) {
            layer.setLayerVisible( currentLayerVisible );
        }

        // Make the Default Layer Active if the current Layer is both Active and
        // Hidden otherwise, unless we are acting on the Default Layer already.
        if ( ( currentLayerIndex != DEFAULT_LAYER_INDEX ) && !currentLayerVisible ) {
            final int activeLayerIndex = getActiveLayerIndex( layerCollection );
            if ( activeLayerIndex == currentLayerIndex ) {
                enforceActiveLayerPolicy( layerCollection, DEFAULT_LAYER_INDEX, true );
            }
        }
    }

    // Enforce the Hidden Layer Policy, which is that only a Hidden Layer cannot
    // be Active. Default to the Default Layer if the current Layer is Active
    // and we are trying to set it to Hidden.
    public static void enforceHiddenLayerPolicy(
            final List< Layer > layerCollection,
            final String currentLayerName,
            final boolean currentLayerVisible ) {
        final int currentLayerIndex = getLayerIndex(
                layerCollection, currentLayerName );
        enforceHiddenLayerPolicy(
                layerCollection, currentLayerIndex, currentLayerVisible );
    }

    public static Layer getActiveLayer( final List< Layer > layerCollection ) {
        for ( final Layer layer : layerCollection ) {
            if ( layer.isLayerActive() ) {
                return layer;
            }
        }

        return getDefaultLayer( layerCollection );
    }

    public static Layer getActiveLayer( final List< Layer > layerCollection,
                                        final String activeLayerName ) {
        return getLayerByName( layerCollection, activeLayerName );
    }

    public static int getActiveLayerIndex(
            final List< Layer > layerCollection ) {
        final Layer activeLayer = getActiveLayer( layerCollection );

        return getLayerIndex( layerCollection, activeLayer );
    }

    public static String getActiveLayerName(
            final List< Layer > layerCollection ) {
        final Layer activeLayer = getActiveLayer( layerCollection );

        return activeLayer.getLayerName();
    }

    // Get the observable drop-list of assignable Layer Names.
    public static List< String > getAssignableLayerNames(
            final List< Layer > layerCollection,
            final boolean supportMultiEdit ) {
        final List< String > layerNames = new ArrayList<>();

        // Preface the necessary "various" label for heterogeneous selections.
        // NOTE: This is only relevant if we support multi-edit.
        if ( supportMultiEdit ) {
            layerNames.add( VARIOUS_LAYER_NAME );
        }

        // Get the current name for each visible Layer. Enforce uniqueness.
        for ( final Layer layer : layerCollection ) {
            if ( layer.isLayerVisible() ) {
                final String layerName = layer.getLayerName();
                if ( ( layerName != null ) && !layerName.trim().isEmpty() ) {
                    if ( !layerNames.contains( layerName ) ) {
                        layerNames.add( layerName );
                    }
                }
            }
        }

        return layerNames;
    }

    public static Layer getDefaultLayer( final List< Layer > layerCollection ) {
        return layerCollection.get( DEFAULT_LAYER_INDEX );
    }

    public static Layer getLayer( final List< Layer > layerCollection,
                                  final int layerIndex ) {
        return !isLayerIndexValid( layerCollection, layerIndex )
            ? null
            : layerCollection.get( layerIndex );
    }

    // Get the Layer from the referenced Layer collection corresponding to the
    // name of the provided Layer, or the Default Layer if invalid Layer.
    public static Layer getLayerByName( final List< Layer > layerCollection,
                                        final Layer layer ) {
        final String layerName = layer.getLayerName();
        return getLayerByName( layerCollection, layerName );
    }

    // Get the Layer from the referenced Layer collection corresponding to the
    // provided Layer Name, or the Default Layer if invalid Layer Name.
    public static Layer getLayerByName( final List< Layer > layerCollection,
                                        final String layerName ) {
        if ( ( layerCollection != null ) && ( layerName != null ) && !layerName.trim().isEmpty() ) {
            for ( final Layer layer : layerCollection ) {
                if ( layer.getLayerName().equals( layerName ) ) {
                    return layer;
                }
            }
        }

        final Layer defaultLayer = ( layerCollection != null )
            ? getDefaultLayer( layerCollection )
            : makeDefaultLayer();

        return defaultLayer;
    }

    // Get a cloned Layer; accounting for uniqueness and business logic.
    public static Layer getLayerClone( final int referenceLayerIndex,
                                       final List< Layer > layerCollection ) {
        final Layer referenceLayer = getLayer( layerCollection, referenceLayerIndex );
        return getLayerClone( referenceLayer, layerCollection );
    }

    // Get a cloned Layer; accounting for uniqueness and business logic.
    public static Layer getLayerClone( final Layer referenceLayer,
                                       final List< Layer > layerCollection ) {
        if ( referenceLayer == null ) {
            return null;
        }

        final String newLayerName = getNewLayerNameDefault( layerCollection );

        final Color color = referenceLayer.getLayerColor();
        final boolean display = referenceLayer.isLayerVisible();
        final boolean lock = referenceLayer.isLayerLocked();

        return new Layer(
                newLayerName,
                color,
                LAYER_STATUS_DEFAULT,
                display,
                lock );
    }

    // Get a defaulted Layer; usually used for setting known values.
    public static Layer getLayerDefault() {
        return new Layer( LAYER_NAME_DEFAULT,
                                    LAYER_COLOR_DEFAULT,
                                    LAYER_STATUS_DEFAULT,
                                    LAYER_DISPLAY_DEFAULT,
                                    LAYER_LOCK_DEFAULT );
    }

    public static int getLayerIndex( final List< Layer > layerCollection,
                                     final Layer layer ) {
        return layerCollection.indexOf( layer );
    }

    public static int getLayerIndex( final List< Layer > layerCollection,
                                     final String layerName ) {
        final Layer layer = getLayerByName( layerCollection, layerName );

        return getLayerIndex( layerCollection, layer );
    }

    // Get the next available Layer Name for a new Layer in the collection.
    public static String getNewLayerNameDefault(
            final List< Layer > layerCollection ) {
        return getNewLayerNameDefault( LAYER_NAME_DEFAULT, layerCollection );
    }

    // Get the next available Layer Name for a new Layer in the collection.
    public static String getNewLayerNameDefault(
            final String layerNameDefault,
            final List< Layer > layerCollection ) {
        // Bump beyond the current count -- as the new Layer hasn't been added
        // to the collection yet -- but account for numbering starting at 0.
        return LabeledObjectManager.getNewLabelDefault( layerCollection, 
                                                        layerNameDefault, 
                                                        " ",
                                                        true );
    }

    public static boolean hasActiveLayer( final List< Layer > layerCollection ) {
        for ( final Layer layer : layerCollection ) {
            if ( layer.isLayerActive() ) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasLayer( final List< Layer > layerCollection,
                                    final Layer referenceLayer ) {
        final String referenceLayerName = referenceLayer.getLayerName();

        for ( final Layer layer : layerCollection ) {
            if ( layer.getLayerName().equals( referenceLayerName ) ) {
                return true;
            }
        }

        return false;
    }

    public static Layer importLayer( final List< Layer > layerCollection,
                                     final Layer layerCandidate ) {
        // Prevent exceptions by filtering for null objects and defaulting.
        if ( layerCandidate == null ) {
            return getDefaultLayer( layerCollection );
        }

        // Add the Layer candidate to the collection if not already present.
        addLayerIfUnique( layerCollection, layerCandidate );

        // Return the Layer candidate as affirmation it is safe to assign.
        return layerCandidate;
    }

    public static boolean isLayerHidden( final List< Layer > layerCollection,
                                         final int layerIndex ) {
        final Layer layer = layerCollection.get( layerIndex );
        return !layer.isLayerVisible();
    }

    public static boolean isLayerIndexValid( final List< Layer > layerCollection,
                                             final int layerIndex ) {
        return ( layerCollection != null ) && ( layerIndex >= 0 )
                && ( layerIndex < layerCollection.size() );
    }

    public static boolean isLayerNameUnique( final String layerNameCandidate,
                                             final List< Layer > layerCollection,
                                             final int excludeLayerIndex ) {
        // Determine name-uniqueness of the supplied Layer Name candidate.
        for ( int layerIndex = 0, numberOfLayers = layerCollection
                .size(); layerIndex < numberOfLayers; layerIndex++ ) {
            if ( ( layerIndex != excludeLayerIndex ) && layerNameCandidate
                    .equals( layerCollection.get( layerIndex ).getLayerName() ) ) {
                return false;
            }
        }

        return true;
    }

    public static Layer makeDefaultLayer() {
        return new Layer(
                DEFAULT_LAYER_NAME,
                LAYER_COLOR_DEFAULT,
                true,
                true,
               false );
    }

    public static List< Layer > makeLayerCollection() {
        // Use the extractor pattern to ensure that edits to the specified
        // properties trigger list change events, as otherwise only adding to
        // and from the list does so, as it doesn't look at the granularity of
        // the observable properties below.
        /*
        final List< Layer > layerCollection = FXCollections
                .observableArrayList( layerProperties -> new Observable[] {
                        layerProperties.layerNameProperty(),
                        layerProperties.layerColorProperty(),
                        layerProperties.layerActiveProperty(),
                        layerProperties.layerVisibleProperty(),
                        layerProperties.layerLockedProperty() } );
        */

        final List< Layer > layerCollection = new ArrayList<>();

        // Set the collection to initially only contain the Default Layer.
        resetLayerCollection( layerCollection );

        return layerCollection;
    }

    public static Layer makeTempLayer() {
        // make a default Layer as the temp layer.
        return new Layer(
                TEMP_LAYER_NAME,
                LAYER_COLOR_DEFAULT,
                false,
                true,
                false );
    }

    public static void reassignObjectOnDeletedLayer(
            final LayerAssignable layerObject,
            final List< Layer > layerCollection,
            final Layer activeLayer ) {
        final Layer objectLayer = layerObject.getLayer();
        if ( !hasLayer( layerCollection, objectLayer ) ) {
            layerObject.setLayer( activeLayer );
        }
    }

    public static void resetLayerCollection(
            final List< Layer > layerCollection ) {
        final Layer defaultLayer = makeDefaultLayer();
        layerCollection.clear();
        layerCollection.add( defaultLayer );
    }

    public static void setActiveLayer( final Layer activeLayer ) {
        if ( !activeLayer.isLayerActive() ) {
            activeLayer.setLayerActive( true );
        }
    }

    public static Layer setActiveLayer( final List< Layer > layerCollection,
                                        final int activeLayerIndex ) {
        final Layer activeLayer = layerCollection.get( activeLayerIndex );
        setActiveLayer( activeLayer );

        return activeLayer;
    }

    public static Layer setActiveLayer( final List< Layer > layerCollection,
                                        final String activeLayerName ) {
        final Layer activeLayer = getActiveLayer( layerCollection, activeLayerName );
        setActiveLayer( activeLayer );

        return activeLayer;
    }

    public static Layer setDefaultLayerActive(
            final List< Layer > layerCollection ) {
        return setActiveLayer( layerCollection, DEFAULT_LAYER_INDEX );
    }

    public static void uniquefyLayerName( final List< Layer > layerCollection,
                                          final String layerNameCandidate,
                                          final String labelToExclude,
                                          final NumberFormat uniquefierNumberFormat ) {
        // Get the current Layer.
        final Layer layer = getLayerByName( layerCollection, labelToExclude );

        // Get a unique Layer Name from the candidate name.
        // NOTE: Make sure we aren't trying to change the Default Layer Name.
        final String oldLayerName = layer.getLayerName();
        final String newLayerName = ( DEFAULT_LAYER_NAME == labelToExclude )
            ? DEFAULT_LAYER_NAME
            : LabeledObjectManager.getUniqueLabel( layerCollection,
                                                   layerNameCandidate,
                                                   labelToExclude,
                                                   uniquefierNumberFormat );

        // If user edits were dismissed -- resulting in no consequent change
        // from the pre-edit state -- pre-cache the unadjusted, candidate value,
        // as otherwise we can miss some view-syncing due to no "changed" event.
        final boolean layerNameChanged = !newLayerName.equals( oldLayerName );
        if ( !layerNameChanged ) {
            layer.setLayerName( layerNameCandidate );
        }

        // Unconditionally cache the corrected Layer Name, as the only edge case
        // is when it results in cancellation of edits, but we still have to set
        // to the interim candidate value first or we get no view refresh due to
        // no "changed" event when the start value equals the end value.
        layer.setLayerName( newLayerName );
    }

    // NOTE: The constructor is disabled, as this is a static class.
    private LayerManager() {}
}
