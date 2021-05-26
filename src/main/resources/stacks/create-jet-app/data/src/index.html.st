<!DOCTYPE html>
<html lang="en-us">

<head>
  <title>Oracle JET Starter Template - Web Basic</title>

  <meta charset="UTF-8">
  <meta name="viewport" content="viewport-fit=cover, width=device-width, initial-scale=1">
  <link rel="icon" href="images/favicon.ico" type="image/x-icon" />

  <!-- This is the main css file for the default theme -->
  <!-- injector:theme -->
  <link rel="stylesheet" href="css/libs/oj/v10.0.0/redwood/oj-redwood-min.css" type="text/css" />
  <!-- endinjector -->
  <!-- This contains icon fonts used by the starter template -->
  <link rel="stylesheet" href="css/demo-alta-site-min.css" type="text/css" />
  <link rel="stylesheet" id="uxiconFont"
    href="https://static.oracle.com/cdn/fnd/gallery/2010.0.0/images/iconfont/ojuxIconFont.min.css">

  <!-- This is where you would add any app specific styling -->
  <link rel="stylesheet" href="css/app.css" type="text/css" />

  <script src="./js/piLibs.js"></script>
  <script src="./js/effect.js"></script>
</head>

<body class="oj-web-applayout-body" onload="startEffect();">
  <div id="globalBody" class="oj-web-applayout-page">
    <!--
         ** Oracle JET V9.2.0 web application header pattern.
         ** Please see the demos under Cookbook/Patterns/App Shell: Web
         ** and the CSS documentation under Support/API Docs/Non-Component Styling
         ** on the JET website for more information on how to use this pattern.
      -->
    <div role="main" class="oj-web-applayout-max-width oj-web-applayout-content">
      <!-- Place your page content here -->
      <div class="oj-flex">
        <div class="oj-flex oj-sm-flex-items-initial demo-center-panels">
          <div class="tiers-bar oj-panel oj-panel-shadow-lg oj-sm-margin-2x demo-panel-customizations oj-flex-item center">
            <img src="images/ojet-logo.png" class="center app-logo" alt="Oracle JET framework logo"></img>
            <p class="center">
              Edit <code>index.html</code> or <code>appController.js</code> and save to reload.
            </p>
            <a
              class="center app-link"
              href="http://www.oraclejet.org"
              target="_blank"
              rel="noopener noreferrer"
            >
                Learn Oracle Jet
            </a>
          </div>

          <div class="tiers-bar oj-panel oj-panel-shadow-lg oj-sm-margin-2x demo-panel-customizations oj-flex-item">
            <img src="images/adb.png" class="center ADB-logo" alt="Autonomous Database logo"></img>
            <div class="center">
              <p class="small-text">
                <oj-bind-text value="[[numberOfDocuments]]"></oj-bind-text> documents in <oj-bind-text value="[[numberOfCollections]]"></oj-bind-text> collections.
              </p>
              <a
                class="center app-link"
                href="https://www.oracle.com/database/technologies/appdev/rest.html"
                target="_blank"
                rel="noopener noreferrer"
              >
                Learn Oracle Rest Data Services
              </a>
            </div>
            <div class="tools-bar">
              <a class="img-flash-effect" href="https://docs.oracle.com/en/database/oracle/simple-oracle-document-access/rest/adrst/loe.html" target="_blank">
                <figure><img src="images/soda4resthelp.png" alt="Simple Oracle Document Access list of examples" title="Use REST APIs"></img></figure>
              </a>
              <a class="img-flash-effect" href="%config.sqlDevWeb%" target="_blank">
                <figure><img src="images/sqldeveloperweb.png" alt="SQL Developer Web" title="Manage Your Data"></img></figure>
              </a>
              <a class="img-flash-effect" href="%config.databaseServiceURL%" target="_blank">
                <figure><img src="images/ociwebconsole.png" alt="OCI Web Console" title="Monitor"></img></figure>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
    <footer class="app-footer" role="contentinfo">
        <a
          class="app-link"
          href="https://github.com/loiclefevre/dragon"
          target="_blank"
          rel="noopener noreferrer"
        >
          <img src="images/dragon-logo.png" class="Dragon-logo" alt="DRAGON Stack logo" title="DRAGON Stack" />
        </a>
        <p class="credits">Background inspired from this <a
          class="app-link"
          href="https://www.shadertoy.com/view/MdfGRX"
          target="_blank"
          rel="noopener noreferrer"
        >
          shader
        </a> by <a
          class="app-link"
          href="http://iquilezles.org/"
          target="_blank"
          rel="noopener noreferrer"
        >
          I&ntilde;igo Quilez
        </a>
      </p>
    </footer>
  </div>

  <script type="text/javascript" src="js/libs/require/require.js"></script>
  <script type="text/javascript" src="js/main.js"></script>

  <canvas id="background" class="app-background" width="100\%" height="100\%" />

</body>

</html>
