package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class NotAuthorizedOrNotFoundException extends DSException {
	public NotAuthorizedOrNotFoundException(String profileName, String compartmentId) {
		super(ErrorCode.NotAuthorizedOrNotFound, String.format("You don't have access to the resources in the compartment designed by the parameter %s provided in the \"%s\" file%s!\n" +
						"Please ensure that you belong to an Oracle Cloud Infrastructure group (e.g. DRAGON_Stack_Developers in the following example) having at least the following policies:\n\n" +
						"Allow group DRAGON_Stack_Developers to manage buckets in compartment id %s\n" +
						"Allow group DRAGON_Stack_Developers to manage objects in compartment id %s\n" +
						"Allow group DRAGON_Stack_Developers to manage autonomous-database-family in compartment id %s\n" +
						"Allow group DRAGON_Stack_Developers to inspect work-requests in compartment id %s\n" +
						"Allow group DRAGON_Stack_Developers to read resource-availability in compartment id %s\n" +
						"Allow group DRAGON_Stack_Developers to use cloud-shell in tenancy\n",
				DSSession.CONFIG_COMPARTMENT_ID, DSSession.CONFIGURATION_FILENAME, profileName == null ? "" : " for profile "+profileName,
				compartmentId,compartmentId,compartmentId,compartmentId,compartmentId));
	}
}
