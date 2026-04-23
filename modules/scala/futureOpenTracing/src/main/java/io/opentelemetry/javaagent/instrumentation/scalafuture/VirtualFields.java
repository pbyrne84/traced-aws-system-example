package io.opentelemetry.javaagent.instrumentation.scalafuture;

import io.opentelemetry.javaagent.shaded.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.bootstrap.executors.PropagatedContext;

public final class VirtualFields {

    public static final VirtualField<Runnable, PropagatedContext> RUNNABLE_PROPAGATED_CONTEXT =
            VirtualField.find(Runnable.class, PropagatedContext.class);

    private VirtualFields() {}
}