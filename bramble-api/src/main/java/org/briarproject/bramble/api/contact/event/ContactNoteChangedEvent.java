package org.briarproject.bramble.api.contact.event;

import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when the alias for a contact changed.
 */
@Immutable
@NotNullByDefault
public class ContactNoteChangedEvent extends Event {

	private final ContactId contactId;
	@Nullable
	private final String note;

	public ContactNoteChangedEvent(ContactId contactId,
			@Nullable String note) {
		this.contactId = contactId;
		this.note = note;
	}

	public ContactId getContactId() {
		return contactId;
	}

	@Nullable
	public String getNote() {
		return note;
	}
}
