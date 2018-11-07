package org.briarproject.briar.api.introduction;

import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorInfo;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.api.client.SessionId;
import org.briarproject.briar.api.conversation.ConversationMessageVisitor;
import org.briarproject.briar.api.conversation.ConversationResponse;

import javax.annotation.concurrent.Immutable;

import static org.briarproject.briar.api.introduction.Role.INTRODUCER;

@Immutable
@NotNullByDefault
public class IntroductionResponse extends ConversationResponse {

	private final Author introducedAuthor;
	private final AuthorInfo introducedAuthorInfo;
	private final Role ourRole;

	public IntroductionResponse(MessageId messageId, GroupId groupId, long time,
			boolean local, boolean sent, boolean seen, boolean read,
			SessionId sessionId, boolean accepted, Author author,
			AuthorInfo introducedAuthorInfo, Role role) {
		super(messageId, groupId, time, local, sent, seen, read, sessionId,
				accepted);
		this.introducedAuthor = author;
		this.introducedAuthorInfo = introducedAuthorInfo;
		this.ourRole = role;
	}

	public Author getIntroducedAuthor() {
		return introducedAuthor;
	}

	public AuthorInfo getIntroducedAuthorInfo() {
		return introducedAuthorInfo;
	}

	public boolean isIntroducer() {
		return ourRole == INTRODUCER;
	}

	@Override
	public <T> T accept(ConversationMessageVisitor<T> v) {
		return v.visitIntroductionResponse(this);
	}
}
