

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.server;

import java.util.Map;

import stonesoup.database.DbConnector;
import stonesoup.database.DbCrudHandler;


class DoQuery extends ClientConnection
{
	DbConnector connector = null;

	public DoQuery(DbConnector _connector)
	{
		this.connector = _connector;
	}

	/**
	 * Passes the parsed command to the DbCrudHandler to execute against the database.
	 * @param mapParams
	 * @return
	 */
	protected String doQuery(final Map<String,String> mapParams)
	{
		String operation = null, value = null, id = null, in_pswd = "";
		double authorized = 0;
		Object obj_id = null;

		if(mapParams.containsKey(PARAM_OPERATION)){
			operation = mapParams.get(PARAM_OPERATION);
		}
		if(mapParams.containsKey(PARAM_ID)){
			id = mapParams.get(PARAM_ID);
			try{
				obj_id = new Integer(id);
			}
			catch(NumberFormatException nfe)
			{
				System.out.println("Number Format Problem");
			}
		}
		if(mapParams.containsKey(PARAM_VALUE)){
			value = mapParams.get(PARAM_VALUE);
		}
		if(mapParams.containsKey(PARAM_PSWD)){
			in_pswd = mapParams.get(PARAM_PSWD);
		}
		if(mapParams.containsKey(PARAM_AUTHORIZED)){
			if(Integer.parseInt(mapParams.get(PARAM_AUTHORIZED)) == 1){
				authorized = 1;
			}
		}

		String response = "";

		if(operation != null)
		{
			DbCrudHandler crudHandler = new DbCrudHandler(this.connector);

			if(operation.equals(PARAM_OPERATION_UPDATE))
			{
				if(id != null && value != null)
				{
					boolean success = crudHandler.doUpdate(id, value);	//STONESOUP:TRIGGER_POINT
					response = (success) ? "update successful" : "update failed";
				}
				else
				{
					System.err.println("!! need an id and a value to perform an update operation" );
				}
			}
			else if(operation.equals(PARAM_OPERATION_INSERT))
			{
				if(value != null)
				{
					String newId = crudHandler.doInsert(value);
					response = (newId != null) ? "insert successful: new id: " + newId : "insert unsuccessful";
				}
				else
				{
					System.err.println("!! need a value to perform an insert operation");
				}
			}
			else if(operation.equals(PARAM_OPERATION_DELETE))
			{
				
				boolean validPswd = in_pswd.equals(pswd);
				if(validPswd)
				{
					authorized = 1;
				}
				else
				{
					response = "Incorrect password";
				}

				if(obj_id != null)
				{
					if(authorized != 1)
					{
						System.out.println("** not authorized to perform delete");
					}
					else
					{
						boolean success = crudHandler.doDelete(obj_id.toString());
						response = (success) ? "delete successful" : "delete unsuccessful";
					}
				}
				else
				{
					System.err.println("!! need an id to perform a delete operation");
				}
			}
			else
			{
				System.err.println("!! unrecognized operation specified by client. so confused.");
			}
		}
		else
		{
			System.err.println("!! no operation specified by client. so confused.");
		}

		return response;
	}
}
