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
      return await axios.post(process.env.REACT_APP_SQL_API, 'SELECT count(*) as "numberOfDocuments" FROM ' + collectionName, {
          auth: {
            username: process.env.REACT_APP_DATABASE_USER_NAME,
            password: process.env.REACT_APP_DATABASE_USER_PASSWORD
          },
          headers: {
            'content-type': 'application/sql'
          }
        })
        .then(res => res.data.items[0].resultSet.items[0].numberofdocuments)
        .catch(err => err);
  }

export default new ORDS();

