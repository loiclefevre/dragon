import dragonlogo from './dragon-logo.png';
import ociwebconsole from './ociwebconsole.png';
import sqldeveloperweb from './sqldeveloperweb.png';
import soda4resthelp from './soda4resthelp.png';
import reactlogo from './logo.svg';
import autonomousdatabaselogo from './adb.png';
import './App.css';
import SODACollections from './components/SODACollections'
import Background from './components/Background'

function App() {
  return (
    <div className="App">
      <Background />
      <header className="App-header">
        <div className="Tiers-bar">
          <img src={reactlogo} className="App-logo" alt="React logo" />
          <p>
            Edit <code>src/App.js</code> and save to reload.
          </p>
          <a
            className="App-link"
            href="https://reactjs.org"
            target="_blank"
            rel="noopener noreferrer"
          >
            Learn React
          </a>
        </div>
        <div className="Tiers-bar">
          <img src={autonomousdatabaselogo} className="ADB-logo" alt="Autonomous Database logo" />
          <SODACollections />
          <p>
            Use <code>src/ORDS.js</code> for CRUD operations.
          </p>
          <a
            className="App-link"
            href="https://www.oracle.com/database/technologies/appdev/rest.html"
            target="_blank"
            rel="noopener noreferrer"
          >
            Learn ORDS
         </a>
          <div className="Tools-bar">
            <a 
              className="Img-flash-effect App-link"
              href="https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/loe.html" 
              target="_blank"
              rel="noopener noreferrer">
                <figure>
                  <img src={soda4resthelp} alt="Simple Oracle Document Access list of examples" title="Use REST APIs" />
                </figure>
            </a>

            <a 
              className="Img-flash-effect App-link"
              href="%config.sqlDevWeb%"
              target="_blank"
              rel="noopener noreferrer">
                <figure>
                  <img src={sqldeveloperweb} alt="SQL Developer Web" title="Manage Your Data" />
                </figure>
            </a>

            <a 
              className="Img-flash-effect App-link"
              href="%config.databaseServiceURL%"
              target="_blank"
              rel="noopener noreferrer">
                <figure>
                  <img src={ociwebconsole} alt="OCI Web Console" title="Monitor" />
                </figure>
            </a>
          </div>
        </div>

        {/* Uncomment to display framerate per second  
            <div id="fps" className="App-framerate" /> 
          */}
      </header>
     
      <footer className="App-footer">
        <a 
          className="App-link"
          href="https://github.com/loiclefevre/dragon" 
          target="_blank"
          rel="noopener noreferrer"
          >
          <img src={dragonlogo} className="Dragon-logo" alt="DRAGON Stack logo" title="DRAGON Stack" />
        </a>
        <p className="credits">Background inspired from this <a
              className="App-link"
			  href="https://www.shadertoy.com/view/MdfGRX"
              target="_blank"
              rel="noopener noreferrer"
			  >
			  shader
			</a> by <a
              className="App-link"
			  href="http://iquilezles.org/"
              target="_blank"
              rel="noopener noreferrer"
			  >
			  Iñigo Quilez
			</a>
		</p>
      </footer>
    </div>
  );
}

export default App;
