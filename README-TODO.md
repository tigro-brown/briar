These are a collection of files that may be useful to read into further and/or edit

## General
- [strings.xml](briar-android/src/main/res/values/strings.xml)
  - the main strings.xml file

### Notes panel
- [conversation_actions.xml](briar-android/src/main/res/menu/conversation_actions.xml)
  - Layout file, has been edited to show a notes pane 
- [ConversationActivity.java](briar-android/src/main/java/org/briarproject/briar/android/conversation/ConversationActivity.java)
  - Handles click on the "Add personal note" component
- [AliasDialogFragment.java](briar-android/src/main/java/org/briarproject/briar/android/conversation/AliasDialogFragment.java)
  - Copy this format when handling the "Add personal note" click
- [NotesDialogFragment.java](briar-android/src/main/java/org/briarproject/briar/android/conversation/NotesDialogFragment.java)
  - See above
- [ActivityComponent.java](briar-android/src/main/java/org/briarproject/briar/android/activity/ActivityComponent.java)
  - Had to add a single line (see below), may need to make future adjustments
  - `void inject(NotesDialogFragment notesDialogFragment);`
- [fragment_notes_dialog.xml](briar-android/src/main/res/layout/fragment_notes_dialog.xml)
  - modeled after fragment_alias_dialog.xml, opened by NotesDialogFragment.java
- [ConversationViewModel.java](briar-android/src/main/java/org/briarproject/briar/android/conversation/ConversationViewModel.java)
  - viewModel for NotesDialogFragment
- [Contact.java](bramble-api/src/main/java/org/briarproject/bramble/api/contact/Contact.java)
  - stores contact information

### Nuclear password
- [PanicResponderActivity.java](briar-android/src/main/java/org/briarproject/briar/android/panic/PanicResponderActivity.java)
  - I think this calls the panic response
- This code `signOut(true, false);` needs to be called from a class that extends BriarActivity