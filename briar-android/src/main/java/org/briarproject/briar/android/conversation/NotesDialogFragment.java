package org.briarproject.briar.android.conversation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static java.util.Objects.requireNonNull;
import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.briarproject.bramble.util.StringUtils.toUtf8;
import static org.briarproject.briar.android.util.UiUtils.hideSoftKeyboard;
import static org.briarproject.briar.android.util.UiUtils.showSoftKeyboard;

/**
 * This fragment helps provide a notes view, such that users can save notes
 * about the people they add as contacts
 */

// TODO: currently, none of this is implemented correctly

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class NotesDialogFragment extends AppCompatDialogFragment {

	final static String TAG = NotesDialogFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private ConversationViewModel viewModel;
	private TextInputLayout notesEditLayout;
	private EditText notesEditText;

	public static NotesDialogFragment newInstance() {
		return new NotesDialogFragment();
	}

	@Override
	public void onAttach(Context ctx) {
		super.onAttach(ctx);
		injectFragment(
				((BaseActivity) requireActivity()).getActivityComponent());
	}

	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
				.get(ConversationViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(STYLE_NO_TITLE, R.style.BriarDialogTheme);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_notes_dialog, container,
				false);

		notesEditLayout = v.findViewById(R.id.notesEditLayout);
		notesEditText = v.findViewById(R.id.notesEditText);
		Contact contact = requireNonNull(viewModel.getContactItem().getValue())
				.getContact();
		// TODO: probably need to change this - edit: should be done
		String notes = contact.getNotes();
		System.out.println("test notes: " + notes);
		notesEditText.setText(notes);
		if (notes != null) notesEditText.setSelection(notes.length());

		Button setButton = v.findViewById(R.id.setButton);
		setButton.setOnClickListener(v1 -> onSetButtonClicked());

		Button cancelButton = v.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(v1 -> onCancelButtonClicked());

		return v;
	}

	private void onSetButtonClicked() {
		System.out.println("setBtn clicked");
		hideSoftKeyboard(notesEditText);
		String note = notesEditText.getText().toString().trim();
		if (toUtf8(note).length > MAX_AUTHOR_NAME_LENGTH) {
			notesEditLayout.setError(getString(R.string.name_too_long));
		} else {
			// TODO: need to change this too, not setContact
			viewModel.setContactNote(note);
			getDialog().dismiss();
		}
	}

	private void onCancelButtonClicked() {
		hideSoftKeyboard(notesEditText);
		getDialog().cancel();
	}

	@Override
	public void onStart() {
		super.onStart();
		requireNonNull(getDialog().getWindow())
				.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		showSoftKeyboard(notesEditText);
	}

}
