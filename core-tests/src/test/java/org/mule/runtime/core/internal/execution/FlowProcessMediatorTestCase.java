/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.execution.SourcePolicyTestUtils.onCallback;
import static org.mule.tck.junit4.matcher.EitherMatcher.leftMatches;
import static org.mule.tck.junit4.matcher.EitherMatcher.rightMatches;
import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorType;
import static org.mule.tck.junit4.matcher.MessagingExceptionMatcher.withEventThat;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractFlowConstruct;
import org.mule.runtime.core.internal.construct.FlowBackPressureMaxConcurrencyExceededException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.internal.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.EventMatcher;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

@SmallTest
public class FlowProcessMediatorTestCase extends AbstractMuleContextTestCase {

  private static final ErrorType ERROR_FROM_FLOW =
      ErrorTypeBuilder.builder().parentErrorType(mock(ErrorType.class)).namespace("TEST").identifier("FLOW_FAILED").build();

  private FlowConstruct flow;
  private Consumer<Exception> flowErrorHandlerRouter;
  private MessageProcessContext context;
  private SourceResultAdapter resultAdapter;
  private FlowProcessTemplate template;
  private PhaseResultNotifier notifier;

  private SourcePolicy sourcePolicy;
  private SourcePolicySuccessResult successResult;
  private SourcePolicyFailureResult failureResult;
  private CoreEvent event;
  private MessagingException messagingException;
  private RuntimeException mockException;

  private FlowProcessMediator flowProcessMediator;
  private final Supplier<Map<String, Object>> failingParameterSupplier = () -> {
    throw mockException;
  };
  private final Function<CoreEvent, Map<String, Object>> failingParameterFunction = event -> {
    throw mockException;
  };
  private PolicyManager policyManager;

  @Parameters
  public static Collection<Object> data() {
    return asList(new Object[] {false, true});
  }

  @Before
  public void before() throws Exception {
    event = mock(CoreEvent.class);
    mockException = mock(RuntimeException.class);

    policyManager = mock(PolicyManager.class);
    sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);
    successResult = mock(SourcePolicySuccessResult.class);
    when(successResult.getResult()).then(invocation -> event);
    when(successResult.getResponseParameters()).thenReturn(() -> emptyMap());
    when(successResult.createErrorResponseParameters()).thenReturn(event -> emptyMap());
    failureResult = mock(SourcePolicyFailureResult.class);
    when(failureResult.getMessagingException()).then(invocation -> messagingException);
    when(failureResult.getResult()).then(invocation -> messagingException.getEvent());
    when(failureResult.getErrorResponseParameters()).thenReturn(() -> emptyMap());
    doAnswer(inv -> {
      event = inv.getArgument(0);
      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback = inv.getArgument(2);
      callback.complete(right(successResult));

      return null;
    }).when(sourcePolicy).process(any(), any(), any());

    flowProcessMediator = new FlowProcessMediator(policyManager);
    initialiseIfNeeded(flowProcessMediator, muleContext);
    startIfNeeded(flowProcessMediator);

    flow = mock(AbstractFlowConstruct.class, withSettings().extraInterfaces(Component.class));
    FlowExceptionHandler exceptionHandler = mock(FlowExceptionHandler.class);

    // Call routeError failure callback for success response sending error test cases
    final ArgumentCaptor<Consumer> propagateConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
    flowErrorHandlerRouter = mock(Consumer.class);
    doAnswer(inv -> {
      propagateConsumerCaptor.getValue().accept(inv.getArgument(0));
      return null;
    })
        .when(flowErrorHandlerRouter).accept(any(Exception.class));
    when(exceptionHandler.router(any(Function.class), any(Consumer.class), propagateConsumerCaptor.capture()))
        .thenReturn(flowErrorHandlerRouter);

    when(((AbstractFlowConstruct) flow).getExceptionListener()).thenReturn(exceptionHandler);
    when(exceptionHandler.apply(any()))
        .thenAnswer(invocationOnMock -> error((MessagingException) invocationOnMock.getArgument(0)));
    when(flow.getMuleContext()).thenReturn(muleContext);

    context = mock(MessageProcessContext.class);
    final MessageSource source = mock(MessageSource.class);
    when(source.getRootContainerLocation()).thenReturn(Location.builder().globalName("root").build());
    when(source.getLocation()).thenReturn(mock(ComponentLocation.class));
    when(context.getMessageSource()).thenReturn(source);
    when(context.getMessagingExceptionResolver()).thenReturn(new MessagingExceptionResolver(source));
    when(context.getTransactionConfig()).thenReturn(empty());
    when(context.getFlowConstruct()).thenReturn(flow);

    template = mock(FlowProcessTemplate.class);
    resultAdapter = mock(SourceResultAdapter.class);
    when(resultAdapter.getResult()).thenReturn(Result.builder().build());
    when(resultAdapter.getMediaType()).thenReturn(ANY);

    when(template.getSourceMessage()).thenReturn(resultAdapter);
    when(template.getNotificationFunctions()).thenReturn(emptyList());

    doAnswer(onCallback(callback -> callback.complete(null))).when(template).sendResponseToClient(any(), any(), any());

    doAnswer(onCallback(callback -> callback.complete(null)))
        .when(template).sendFailureResponseToClient(any(), any(), any());

    notifier = mock(PhaseResultNotifier.class);
  }

  @Test
  public void success() throws Exception {
    flowProcessMediator.process(template, context, notifier);

    verifySuccess();
  }

  @Test
  public void successDelayedResponseCompletion() throws Exception {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    doAnswer(onCallback(callbackReference::set)).when(template).sendResponseToClient(any(), any(), any());

    flowProcessMediator.process(template, context, notifier);

    callbackReference.get().complete(null);
    verifySuccess();
  }

  @Test
  public void successResponseParametersError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);

    flowProcessMediator.process(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(Matchers.any(MessagingException.class))));
    // The error handler is called with the appropriate type, but for the termination it counts as successful if the error
    // response
    // was sent.
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void successResponseSendError() throws Exception {
    doAnswer(onCallback(callback -> callback.error(mockException))).when(template).sendResponseToClient(any(), any(), any());

    flowProcessMediator.process(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseSend());
    verify(template).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    // The error handler is called with the appropriate type, but for the termination it counts as successful if the error
    // response
    // was sent.
    verify(template).afterPhaseExecution(argThat(leftMatches(Matchers.any(MessagingException.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void successResponseParameterAndErrorResponseParameterError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);
    when(successResult.createErrorResponseParameters()).thenReturn(failingParameterFunction);

    flowProcessMediator.process(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(template).afterPhaseExecution(any());
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void successResponseParameterErrorAndErrorResponseSendError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);

    doAnswer(onCallback(callback -> callback.error(mockException)))
        .when(template).sendFailureResponseToClient(any(), any(), any());

    flowProcessMediator.process(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failure() throws Exception {
    configureFailingFlow(mockException);

    flowProcessMediator.process(template, context, notifier);

    verifyFlowError(isErrorTypeFlowFailure());
  }

  @Test
  public void failureInErrorHandler() throws Exception {
    configureErrorHandlingFailingFlow(mockException);

    flowProcessMediator.process(template, context, notifier);

    verifyFlowError(isErrorTypeFlowFailure());
  }

  @Test
  public void failureDelayedErrorResponseSendCompletion() throws Exception {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();

    reset(template);
    when(template.getSourceMessage()).thenReturn(resultAdapter);
    when(template.getNotificationFunctions()).thenReturn(emptyList());
    doAnswer(onCallback(callbackReference::set)).when(template).sendFailureResponseToClient(any(), any(), any());

    configureFailingFlow(mockException);

    flowProcessMediator.process(template, context, notifier);

    callbackReference.get().complete(null);
    verifyFlowError(isErrorTypeFlowFailure());
  }

  @Test
  public void failureDelayedErrorResponseSendFailure() throws Exception {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();

    configureFailingFlow(mockException);
    doAnswer(onCallback(callbackReference::set)).when(template).sendFailureResponseToClient(any(), any(), any());

    flowProcessMediator.process(template, context, notifier);

    verify(template, never()).afterPhaseExecution(any());

    callbackReference.get().error(mockException);

    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failureErrorResponseParameterError() throws Exception {
    configureFailingFlow(mockException);
    when(failureResult.getErrorResponseParameters()).thenReturn(failingParameterSupplier);

    flowProcessMediator.process(template, context, notifier);

    verify(((AbstractFlowConstruct) flow).getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failureErrorResponseSendError() throws Exception {
    configureFailingFlow(mockException);
    doAnswer(onCallback(callback -> callback.error(mockException))).when(template)
        .sendFailureResponseToClient(any(), any(), any());

    flowProcessMediator.process(template, context, notifier);

    verify(((AbstractFlowConstruct) flow).getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failurePolicyManager() throws Exception {
    final ArgumentCaptor<CoreEvent> eventCaptor = ArgumentCaptor.forClass(CoreEvent.class);
    when(policyManager.createSourcePolicyInstance(any(Component.class), eventCaptor.capture(), any(ReactiveProcessor.class),
                                                  any(MessageSourceResponseParametersProcessor.class))).thenThrow(mockException);
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(coreEvent -> emptyMap());

    flowProcessMediator.process(template, context, notifier);

    verify(((AbstractFlowConstruct) flow).getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));

    assertThat(((BaseEventContext) eventCaptor.getValue().getContext()).isTerminated(), is(true));
  }

  @Test
  public void backpressureCheckFailure() throws MuleException {
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(coreEvent -> emptyMap());
    final ArgumentCaptor<CoreEvent> eventCaptor = ArgumentCaptor.forClass(CoreEvent.class);
    doThrow(propagate(new FlowBackPressureMaxConcurrencyExceededException("flow", MAX_CONCURRENCY_EXCEEDED))).when(flow)
        .checkBackpressure(eventCaptor.capture());

    flowProcessMediator.process(template, context, notifier);

    assertThat(((BaseEventContext) eventCaptor.getValue().getContext()).isTerminated(), is(true));
    verifyFlowError(isErrorTypeBackpressure());
  }

  private void verifySuccess() {
    verify(((AbstractFlowConstruct) flow).getExceptionListener(), never()).handleException(any(), any());
    verify(template).sendResponseToClient(any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(rightMatches(Matchers.any(CoreEvent.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
    verify(template).afterPhaseExecution(any());
  }

  private void verifyFlowError(EventMatcher errorTypeMatcher) {
    verify(((AbstractFlowConstruct) flow).getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(withEventThat(errorTypeMatcher))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  private void verifyFlowErrorHandler(final EventMatcher errorHandlerEventMatcher) {
    verify(flowErrorHandlerRouter).accept(argThat(withEventThat(errorHandlerEventMatcher)));
  }

  private EventMatcher isErrorTypeSourceResponseGenerate() {
    return hasErrorType(SOURCE_RESPONSE_GENERATE.getNamespace(), SOURCE_RESPONSE_GENERATE.getName());
  }

  private EventMatcher isErrorTypeSourceResponseSend() {
    return hasErrorType(SOURCE_RESPONSE_SEND.getNamespace(), SOURCE_RESPONSE_SEND.getName());
  }

  private EventMatcher isErrorTypeFlowFailure() {
    return hasErrorType(ERROR_FROM_FLOW.getNamespace(), ERROR_FROM_FLOW.getIdentifier());
  }

  private EventMatcher isErrorTypeBackpressure() {
    return hasErrorType(FLOW_BACK_PRESSURE.getNamespace(), FLOW_BACK_PRESSURE.getName());
  }

  private EventMatcher isErrorTypeSourceErrorResponseGenerate() {
    return hasErrorType(SOURCE_ERROR_RESPONSE_GENERATE.getNamespace(), SOURCE_ERROR_RESPONSE_GENERATE.getName());
  }

  private EventMatcher isErrorTypeSourceErrorResponseSend() {
    return hasErrorType(SOURCE_ERROR_RESPONSE_SEND.getNamespace(), SOURCE_ERROR_RESPONSE_SEND.getName());
  }

  private void configureThrowingFlow(RuntimeException failure, boolean inErrorHandler) {
    doAnswer(invocation -> {
      messagingException = buildFailingFlowException(invocation.getArgument(0), failure);
      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback = invocation.getArgument(2);
      callback.complete(left(failureResult));

      return null;
    }).when(sourcePolicy).process(any(), any(), any());
  }

  private void configureFailingFlow(RuntimeException failure) {
    configureThrowingFlow(failure, false);
  }

  private void configureErrorHandlingFailingFlow(RuntimeException failure) {
    configureThrowingFlow(failure, true);
  }


  private MessagingException buildFailingFlowException(final CoreEvent event, final Exception exception) {
    return new MessagingException(CoreEvent.builder(event)
        .error(ErrorBuilder.builder(exception).errorType(ERROR_FROM_FLOW).build())
        .build(), exception);
  }

}
