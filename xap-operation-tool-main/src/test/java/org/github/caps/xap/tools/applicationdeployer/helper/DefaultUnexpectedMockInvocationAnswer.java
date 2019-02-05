package org.github.caps.xap.tools.applicationdeployer.helper;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultUnexpectedMockInvocationAnswer implements Answer {

	public static final DefaultUnexpectedMockInvocationAnswer singleton = new DefaultUnexpectedMockInvocationAnswer();

	@Override
	public Object answer(InvocationOnMock invocationOnMock) {
		throw new IllegalArgumentException(invocationOnMock.toString());
	}
}
