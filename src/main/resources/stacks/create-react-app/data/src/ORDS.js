import axios from 'axios';

function ORDS() {
}

ORDS.prototype.getCollections = async function() {
        return await axios.get(process.env.REACT_APP_SODA_API, {
            auth: {
              username: process.env.REACT_APP_DATABASE_USER_NAME,
              password: process.env.REACT_APP_DATABASE_USER_PASSWORD
            }
          })
          .then(res => res.data)
          .catch(err => err);
    }

ORDS.prototype.getNumberOfDocumentsInCollection = async function(collectionName) {
      return await axios.post(process.env.REACT_APP_SQL_API,
		`{
			"statementText":
				"DECLARE
				 table_count number;
				 table_name DBMS_QUOTED_ID;
				 valid_table_name DBMS_QUOTED_ID;
				 BEGIN
					table_name := :table_name_input;
					valid_table_name := SYS.DBMS_ASSERT.simple_sql_name(table_name);
					execute immediate 'select count(*) from '|| valid_table_name into table_count;
					:numberOfDocuments := table_count;
				END;",
			"binds":[
				{
					"name": "table_name_input",
					"data_type":"VARCHAR",
					"value":"`+ collectionName +`"
				},
				{
					"name":"numberOfDocuments",
					"data_type":"NUMBER",
					"mode":"out"
				}
			]
		}`,
		{
          auth: {
            username: process.env.REACT_APP_DATABASE_USER_NAME,
            password: process.env.REACT_APP_DATABASE_USER_PASSWORD
          },
          headers: {
            'content-type': 'application/json'
          }
        })
        .then(res => res.data.items[0].binds[1].result)
        .catch(err => err);
  }

export default new ORDS();

