import React from 'react';
import ORDS from '../ORDS';

export class SODACollections extends React.Component {
  state = { numberOfCollections: 0, numberOfDocuments: 0 };
  
  async componentDidMount() {
    var collections = (await ORDS.getCollections()).items;
    var totalDocs = 0;
    for( var i=0; i<collections.length; i++ ) {
        totalDocs += await ORDS.getNumberOfDocumentsInCollection(collections[i].name);
        this.setState({ numberOfCollections: collections.length, numberOfDocuments: totalDocs });
      }
  }

  render() {
    return (
      <div>
        <p className="small-text">{this.state.numberOfDocuments} document(s) in {this.state.numberOfCollections} collection(s)</p>
      </div>
    );
  }
}

export default SODACollections;
