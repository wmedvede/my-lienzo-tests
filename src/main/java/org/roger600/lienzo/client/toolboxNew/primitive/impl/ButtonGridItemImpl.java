package org.roger600.lienzo.client.toolboxNew.primitive.impl;

import java.util.Iterator;

import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.event.NodeDragMoveHandler;
import com.ait.lienzo.client.core.event.NodeDragStartHandler;
import com.ait.lienzo.client.core.event.NodeMouseClickHandler;
import com.ait.lienzo.client.core.event.NodeMouseEnterEvent;
import com.ait.lienzo.client.core.event.NodeMouseEnterHandler;
import com.ait.lienzo.client.core.event.NodeMouseExitEvent;
import com.ait.lienzo.client.core.event.NodeMouseExitHandler;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.Shape;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.tooling.nativetools.client.event.HandlerRegistrationManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import org.roger600.lienzo.client.toolboxNew.GroupVisibilityExecutors;
import org.roger600.lienzo.client.toolboxNew.grid.Point2DGrid;
import org.roger600.lienzo.client.toolboxNew.primitive.AbstractDecoratedItem;
import org.roger600.lienzo.client.toolboxNew.primitive.AbstractDecoratorItem;
import org.roger600.lienzo.client.toolboxNew.primitive.AbstractPrimitiveItem;
import org.roger600.lienzo.client.toolboxNew.primitive.ButtonGridItem;
import org.roger600.lienzo.client.toolboxNew.primitive.DecoratedItem;
import org.roger600.lienzo.client.toolboxNew.primitive.DecoratorItem;
import org.roger600.lienzo.client.toolboxNew.util.BiConsumer;
import org.roger600.lienzo.client.toolboxNew.util.Supplier;

public class ButtonGridItemImpl
        extends WrappedItem<ButtonGridItem>
        implements ButtonGridItem {

    private static final int TIMER_DELAY_MILLIS = 500;

    public static class DropDownFactory {

        private static final double ANIMATION_DELAY_MILLIS = 250;

        public static ButtonGridItem dropDown(final Shape<?> shape) {
            final ButtonGridItemImpl button = new ButtonGridItemImpl(shape);
            return setupAsDropDown(button);
        }

        public static ButtonGridItem dropDown(final Group group) {
            final ButtonGridItemImpl button = new ButtonGridItemImpl(group);
            return setupAsDropDown(button);
        }

        public static ButtonGridItem dropRight(final Shape<?> shape) {
            final ButtonGridItemImpl button = new ButtonGridItemImpl(shape);
            return setupAsDropRight(button);
        }

        public static ButtonGridItem dropRight(final Group group) {
            final ButtonGridItemImpl button = new ButtonGridItemImpl(group);
            return setupAsDropRight(button);
        }

        private static ButtonGridItem setupAsDropDown(final ButtonGridItemImpl button) {
            button.at(Direction.SOUTH_WEST);
            button.useShowExecutor(GroupVisibilityExecutors.upScaleY().setAnimationDuration(ANIMATION_DELAY_MILLIS));
            button.useHideExecutor(GroupVisibilityExecutors.downScaleY().setAnimationDuration(ANIMATION_DELAY_MILLIS));
            return button;
        }

        private static ButtonGridItem setupAsDropRight(final ButtonGridItemImpl button) {
            button.at(Direction.EAST);
            button.useShowExecutor(GroupVisibilityExecutors.upScaleX().setAnimationDuration(ANIMATION_DELAY_MILLIS));
            button.useHideExecutor(GroupVisibilityExecutors.downScaleX().setAnimationDuration(ANIMATION_DELAY_MILLIS));
            return button;
        }
    }

    private final ButtonItemImpl button;
    private final ToolboxImpl toolbox;
    private final Timer unFocusTimer =
            new Timer() {
                @Override
                public void run() {
                    hideGrid(new Runnable() {
                        @Override
                        public void run() {
                            button.getWrapped().unFocus();
                            batch();
                        }
                    });
                }
            };
    private final HandlerRegistration[] decoratorHandlers = new HandlerRegistration[2];

    private ButtonGridItemImpl(final Shape<?> prim) {
        this.button = new ButtonItemImpl(prim);
        this.toolbox = new ToolboxImpl(new DecoratedButtonBoundingBoxSupplier());
        init();
    }

    private ButtonGridItemImpl(final Group group) {
        this.button = new ButtonItemImpl(group);
        this.toolbox = new ToolboxImpl(new DecoratedButtonBoundingBoxSupplier());
        init();
    }

    public ButtonGridItem at(final Direction at) {
        toolbox.at(at);
        return this;
    }

    public ButtonGridItem offset(final Point2D offset) {
        toolbox.offset(offset);
        return this;
    }

    @Override
    public ButtonGridItem grid(final Point2DGrid grid) {
        toolbox.grid(grid);
        return this;
    }

    @Override
    public ButtonGridItem decorateGrid(final DecoratorItem<?> decorator) {
        if (toolbox.getItems().size() > 0) {
            if (decorator instanceof AbstractDecoratorItem) {
                removeDecoratorHandlers();
                final AbstractDecoratorItem instance = (AbstractDecoratorItem) decorator;
                toolbox.decorate(decorator);
                decoratorHandlers[0] = instance
                        .asPrimitive()
                        .setListening(true)
                        .addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                            @Override
                            public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                                itemFocusCallback.run();
                            }
                        });
                decoratorHandlers[1] = instance.asPrimitive().addNodeMouseExitHandler(new NodeMouseExitHandler() {
                    @Override
                    public void onNodeMouseExit(NodeMouseExitEvent event) {
                        itemUnFocusCallback.run();
                    }
                });
                registrations().register(decoratorHandlers[0]);
                registrations().register(decoratorHandlers[1]);
            }
        } else {
            throw new IllegalStateException("Cannot decorate until no items added.");
        }
        return this;
    }

    @Override
    public ButtonGridItem show(final Runnable before,
                               final Runnable after) {
        button.show(before,
                    after);
        return this;
    }

    @Override
    public ButtonGridItem hide(final Runnable before,
                               final Runnable after) {
        hideGrid(new Runnable() {
            @Override
            public void run() {
                button.hide();
                batch();
            }
        });
        return this;
    }

    @Override
    public ButtonGridItem showGrid() {
        toolbox.show();
        return this;
    }

    @Override
    public ButtonGridItem hideGrid() {
        hideGrid(new Runnable() {
            @Override
            public void run() {
            }
        });
        return this;
    }

    private ButtonGridItem hideGrid(final Runnable after) {
        toolbox.hide(new Runnable() {
                         @Override
                         public void run() {
                         }
                     },
                     after);
        return this;
    }

    @Override
    public ButtonGridItem add(final DecoratedItem... items) {
        toolbox.add(items);
        for (final DecoratedItem item : items) {
            try {
                final AbstractDecoratedItem primitiveItem = (AbstractDecoratedItem) item;
                registerItemFocusHandler(primitiveItem,
                                         itemFocusCallback);
                registerItemUnFocusHandler(primitiveItem,
                                           itemUnFocusCallback);
            } catch (final ClassCastException e) {
                throw new UnsupportedOperationException("The button only supports subtypes " +
                                                                "of " + AbstractDecoratedItem.class.getName());
            }
        }
        return this;
    }

    @Override
    public Iterator<DecoratedItem> iterator() {
        return toolbox.iterator();
    }

    @Override
    public ButtonGridItem onClick(final NodeMouseClickHandler handler) {
        button.onClick(handler);
        return this;
    }

    @Override
    public ButtonGridItem onDragStart(final NodeDragStartHandler handler) {
        button.onDragStart(handler);
        return this;
    }

    @Override
    public ButtonGridItem onDragMove(final NodeDragMoveHandler handler) {
        button.onDragMove(handler);
        return this;
    }

    @Override
    public ButtonGridItem onDragEnd(final NodeDragEndHandler handler) {
        button.onDragEnd(handler);
        return this;
    }

    @Override
    public void destroy() {
        super.destroy();
        removeDecoratorHandlers();
        button.destroy();
        toolbox.destroy();
    }

    @Override
    protected AbstractGroupItem<?> getWrapped() {
        return button.getWrapped();
    }

    private void useShowExecutor(final BiConsumer<Group, Runnable> executor) {
        toolbox.getWrapped().getWrapped().useShowExecutor(executor);
    }

    private void useHideExecutor(final BiConsumer<Group, Runnable> executor) {
        toolbox.getWrapped().getWrapped().useHideExecutor(executor);
    }

    private void init() {
        button.getWrapped().setUnFocusDelay(TIMER_DELAY_MILLIS);
        // Register custom focus/un-focus behaviors.
        registerItemFocusHandler(button,
                                 focusCallback);
        registerItemUnFocusHandler(button,
                                   unFocusCallback);
        // Attach the toolbox's primiitive into the button group.
        this.button.asPrimitive()
                .setDraggable(false)
                .add(toolbox.asPrimitive());
    }

    private void registerItemFocusHandler(final AbstractDecoratedItem item,
                                          final Runnable callback) {
        registrations()
                .register(
                        item.getPrimitive().addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                            @Override
                            public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                                callback.run();
                            }
                        })
                );
    }

    private void registerItemUnFocusHandler(final AbstractDecoratedItem item,
                                            final Runnable callback) {
        registrations()
                .register(
                        item.getPrimitive().addNodeMouseExitHandler(new NodeMouseExitHandler() {
                            @Override
                            public void onNodeMouseExit(NodeMouseExitEvent event) {
                                callback.run();
                            }
                        })
                );
    }

    private HandlerRegistrationManager registrations() {
        return button.getWrapped()
                .registrations();
    }

    private ButtonGridItemImpl focus() {
        stopTimer();
        button.getWrapped().focus();
        showGrid();
        return this;
    }

    private ButtonGridItemImpl unFocus() {
        scheduleTimer();
        return this;
    }

    private final Runnable focusCallback = new Runnable() {
        @Override
        public void run() {
            focus();
        }
    };

    private final Runnable unFocusCallback = new Runnable() {
        @Override
        public void run() {
            unFocus();
        }
    };

    private final Runnable itemFocusCallback = new Runnable() {
        @Override
        public void run() {
            focus();
        }
    };

    private final Runnable itemUnFocusCallback = new Runnable() {
        @Override
        public void run() {
            unFocus();
        }
    };

    private void scheduleTimer() {
        unFocusTimer.schedule(TIMER_DELAY_MILLIS);
    }

    private void stopTimer() {
        unFocusTimer.cancel();
    }

    private void batch() {
        button.asPrimitive().batch();
    }

    private void removeDecoratorHandlers() {
        if (null != decoratorHandlers[0]) {
            decoratorHandlers[0].removeHandler();
        }
        if (null != decoratorHandlers[1]) {
            decoratorHandlers[1].removeHandler();
        }
    }

    // Provides the bounding box of the button plus the decorator, as for further toolbox positioning.
    private class DecoratedButtonBoundingBoxSupplier implements Supplier<BoundingBox> {

        @Override
        public BoundingBox get() {
            final DecoratorItem<?> buttonDecorator = button.getWrapped().getDecorator();
            if (null != buttonDecorator && buttonDecorator instanceof AbstractPrimitiveItem) {
                return ((AbstractPrimitiveItem) buttonDecorator).asPrimitive().getBoundingBox();
            }
            return button.getBoundingBox().get();
        }
    }
}