package org.roger600.lienzo.client.toolboxNew.primitive;

import com.ait.lienzo.client.core.event.NodeMouseEnterEvent;
import com.ait.lienzo.client.core.event.NodeMouseEnterHandler;
import com.ait.lienzo.client.core.event.NodeMouseExitEvent;
import com.ait.lienzo.client.core.event.NodeMouseExitHandler;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.tooling.nativetools.client.event.HandlerRegistrationManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.roger600.lienzo.client.toolboxNew.AbstractGroupItem;
import org.roger600.lienzo.client.toolboxNew.GroupItem;

public abstract class AbstractDefaultItem<T extends AbstractDefaultItem>
        extends AbstractGroupItem<T>
        implements DefaultItem<T> {

    private final HandlerRegistrationManager registrations = new HandlerRegistrationManager();
    private DefaultDecoratorItem<?> decorator;
    private HandlerRegistration mouseEnterHandlerRegistration;
    private HandlerRegistration mouseExitHandlerRegistration;
    private Runnable focusCallback;
    private Runnable unFocusCallback;

    public AbstractDefaultItem() {
        super(new GroupItem());
        asPrimitive().add(getPrimitive());
        initHandlers(getPrimitive());
    }

    public abstract IPrimitive<?> getPrimitive();

    @Override
    public T decorate(final DecoratorItem<?> decorator) {
        try {
            initDecorator((DefaultDecoratorItem<?>) decorator);
        } catch (final ClassCastException e) {
            throw new UnsupportedOperationException("This item only supports decorators " +
                                                            "of type " + DefaultDecoratorItem.class.getName());
        }
        return cast();
    }

    public T focus() {
        if (null != focusCallback) {
            focusCallback.run();
        }
        if (isDecorated()) {
            showDecorator();
        }
        return cast();
    }

    public T unFocus() {
        if (null != unFocusCallback) {
            unFocusCallback.run();
        }
        if (isDecorated()) {
            hideDecorator();
        }
        return cast();
    }

    @Override
    public boolean isVisible() {
        return getGroupItem().isVisible();
    }

    @Override
    public T hide() {
        super.hide();
        hideDecorator();
        return cast();
    }

    @Override
    public T onMouseEnter(final NodeMouseEnterHandler handler) {
        assert null != handler;
        if (null != mouseEnterHandlerRegistration) {
            mouseEnterHandlerRegistration.removeHandler();
        }
        mouseEnterHandlerRegistration = getPrimitive()
                .addNodeMouseEnterHandler(handler);
        register(mouseEnterHandlerRegistration);
        return cast();
    }

    @Override
    public T onMouseExit(final NodeMouseExitHandler handler) {
        assert null != handler;
        if (null != mouseExitHandlerRegistration) {
            mouseExitHandlerRegistration.removeHandler();
        }
        mouseExitHandlerRegistration = getPrimitive()
                .addNodeMouseExitHandler(handler);
        register(mouseExitHandlerRegistration);
        return cast();
    }

    public T onFocus(final Runnable callback) {
        this.focusCallback = callback;
        return cast();
    }

    public T onUnFocus(final Runnable callback) {
        this.unFocusCallback = callback;
        return cast();
    }

    public HandlerRegistrationManager registrations() {
        return registrations;
    }

    private T register(final HandlerRegistration registration) {
        registrations.register(registration);
        return cast();
    }

    @Override
    protected void preDestroy() {
        super.preDestroy();
        initDecorator(null);
        destroyHandlers();
        getPrimitive().removeFromParent();
    }

    private boolean isDecorated() {
        return null != this.decorator;
    }

    private void initDecorator(final DefaultDecoratorItem<?> decorator) {
        if (isDecorated()) {
            this.decorator.destroy();
        }
        this.decorator = decorator;
        if (isDecorated()) {
            attachDecorator();
        }
    }

    private void attachDecorator() {
        final BoundingBox boundingBox = getPrimitive().getBoundingBox();
        decorator.setSize(boundingBox.getWidth(),
                          boundingBox.getHeight());
        asPrimitive().add(decorator.asPrimitive());
        if (isVisible()) {
            showDecorator();
        } else {
            hideDecorator();
        }
    }

    private void showDecorator() {
        if (isDecorated()) {
            this.decorator.show();
        }
    }

    private void hideDecorator() {
        if (isDecorated()) {
            this.decorator.hide();
        }
    }

    private void initHandlers(final IPrimitive<?> prim) {
        prim.setListening(true);
        registrations.register(
                prim.addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                    @Override
                    public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                        focus();
                    }
                })
        );
        registrations.register(
                prim.addNodeMouseExitHandler(new NodeMouseExitHandler() {
                    @Override
                    public void onNodeMouseExit(NodeMouseExitEvent event) {
                        unFocus();
                    }
                })
        );
    }

    private void destroyHandlers() {
        registrations.removeHandler();
    }

    @SuppressWarnings("unchecked")
    private T cast() {
        return (T) this;
    }
}