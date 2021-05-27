import axios from 'axios';

function ORDS() {
}

// SODA for REST documentation:
// ============================
// - https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/index.html
// - list of examples:
//   . https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/using-soda-rest.html
//   . https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/loe.html
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

// SQL Service over ORDS documentation:
// ====================================
// - https://docs.oracle.com/en/database/oracle/oracle-rest-data-services/20.3/aelig/rest-enabled-sql-service.html
//
// The example underneath uses a technic to avoid SQL injection by checking the actual existence of the collectionName.
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
					-- checking table_name / collection name really exists...
					valid_table_name := SYS.DBMS_ASSERT.simple_sql_name(table_name);
					-- run query to count the number of JSON documents
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

function buildArray(src) {
    var result = [];
    for (var i in src) {
        result[i] = src[i].value;
    }

    return result;
}

// SODA for REST documentation:
// ============================
// - https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/index.html
// - list of examples:
//   . https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/using-soda-rest.html
//   . https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/loe.html
ORDS.prototype.getPurchaseOrders = async function(startRow, pageSize) {
        // q={"$orderby":[{"path":"PONumber","datatype":"number","order":"desc"}
        return await axios.get(process.env.REACT_APP_SODA_API+'purchase_orders/?offset='+startRow+'&limit='+pageSize+'&q={"$orderby":[{"path":"PONumber","datatype":"number","order":"asc"}]}', {
            auth: {
              username: process.env.REACT_APP_DATABASE_USER_NAME,
              password: process.env.REACT_APP_DATABASE_USER_PASSWORD
            }
          })
          .then(res => buildArray(res.data.items))
          .catch(err => err);
    }

export default new ORDS();
