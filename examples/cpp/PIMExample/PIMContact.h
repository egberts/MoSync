/* Copyright 2013 David Axmark

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/**
 * @file PIMContact.h
 * @author Bogdan Iusco
 *
 * PIMContact class shows how to:
 * - read, modify and delete field values.
 * - set field value attributes and labels(custom attributes).
 * - remove contact item from the list.
 * - count field values.
 */

#ifndef PIMCONTACT_H_
#define PIMCONTACT_H_

#include <IX_PIM.h>
#include <maapi.h>
#include <MAUtil/String.h>

/**
 * PIMContact class shows how to:
 * - read, modify and delete field values.
 * - set field value attributes and labels(custom attributes).
 * - remove contact item from the list.
 * - count field values.
 */
class PIMContact
{
public:
	/**
	 * Constructor.
	 * @param pimItemHandle Handle to a pim contact item.
	 */
	PIMContact(MAHandle pimItemHandle);

	/**
	 * Destructor.
	 */
	virtual ~PIMContact();

	/**
	 * Print all contact's field values.
	 */
	void printContact();

	/**
	 * Add data to the contact.
	 * All fields must be empty.
	 */
	void addDataToContact();

	/**
	 * Modify the first value of the address field.
	 * Set a custom label for that value.
	 */
	virtual void modifyAddressField();

	/**
	 * Remove a specified field value.
	 * @param fieldID One of the MA_PIM_FIELD_CONTACT constants.
	 * @param index Value's index in the given field.
	 */
	virtual void removeFieldValue(const int fieldID, const int index);

	/**
	 * Get contact handle.
	 * @return The contact's handle.
	 */
	virtual MAHandle getHandle() const;

private:
	/**
	 * Wait for a pointer released event.
	 */
	void waitForClick();

	/**
	 * Print the attribute for a given index field value.
	 * If the attribute is custom will print the label also.
	 * @param field One of the MA_PIM_FIELD constants.
	 * @param index The field's value index.
	 * Must be >= 0 and < maPimItemFieldCount().
	 * @param pointerToFunc A pointer to a function that returns the string
	 * associated with the attribute.
	 */
	void printAttribute(
		const int field,
		const int index,
		MAUtil::String (*pointerToFunc)(const int));

	/**
	 * Print contact name field value.
	 */
	void printContactName();

	/**
	 * Print address field values.
	 */
	void printAddress();

	/**
	 * Print birthday field value.
	 */
	void printBirthday();

	/**
	 * Print email field values.
	 */
	void printEmail();

	/**
	 * Print formatted address field value.
	 */
	void printFormattedAddress();

	/**
	 * Print nickname field value.
	 */
	void printNickname();

	/**
	 * Print note field value.
	 */
	void printNote();

	/**
	 * Print organization field value.
	 */
	void printOrg();

	/**
	 * Print photo field value.
	 */
	void printPhoto();

	/**
	 * Print key field value.
	 */
	void printPublicKey();

	/**
	 * Print public key field value.
	 */
	void printPublicKeyString();

	/**
	 * Print revision field value.
	 */
	void printRevision();

	/**
	 * Print phone field values.
	 */
	void printPhone();

	/**
	 * Print title field value.
	 */
	void printTitle();

	/**
	 * Print UID field value.
	 */
	void printUID();

	/**
	 * Print URL field values.
	 */
	void printURL();

	/**
	 * Print instant message field values.
	 */
	void printIM();

	/**
	 * Print relation field values.
	 */
	void printRelation();

	/**
	 * Print organization info field value.
	 */
	void printOrgInfo();

	/**
	 * Add value to contact name field.
	 */
	void addContactName();

	/**
	 * Add values to address field.
	 */
	void addAddress();

	/**
	 * Add value to birthday field.
	 */
	void addBirthday();

	/**
	 * Add values to email field.
	 */
	void addEmail();

	/**
	 * Add value to nickname field.
	 */
	void addNickname();

	/**
	 * Add value to note field.
	 */
	void addNote();

	/**
	 * Add value to organization field.
	 */
	void addOrg();

	/**
	 * Add value to photo field.
	 */
	void addPhoto();

	/**
	 * Add value to URL field.
	 */
	void addPhotoURL();

	/**
	 * Add values to phone field.
	 */
	void addPhone();

	/**
	 * Add value to title field.
	 */
	void addTitle();

	/**
	 * Add values to URL field.
	 */
	void addURL();

	/**
	 * Add value to instant message field.
	 */
	void addIM();

	/**
	 * Add values to relation field.
	 */
	void addRelation();

	/**
	 * Add value to organization field.
	 */
	void addOrgInfo();

private:
	/**
	 * Handle to a contact item.
	 */
	MAHandle mItemHandle;

	/**
	 * Arguments for getting and setting field values.
	 */
	MA_PIM_ARGS mArgs;

	/**
	 * Used for getting and setting field values.
	 */
	char *mBuffer;
};

#endif /* PIMCONTACT_H_ */
