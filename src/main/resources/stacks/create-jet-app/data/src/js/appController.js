/**
 * @license
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates.
 * Licensed under The Universal Permissive License (UPL), Version 1.0
 * as shown at https://oss.oracle.com/licenses/upl/
 * @ignore
 */
/*
 * Your application specific code will go here
 */
define(['ojs/ojresponsiveutils', 'ojs/ojresponsiveknockoututils', 'knockout', 'ojs/ojmodel', 'text!config.json',
        'ojs/ojknockout'],
  function (ResponsiveUtils, ResponsiveKnockoutUtils, ko, Model, configData) {
    function ControllerViewModel() {

      // Media queries for repsonsive layouts
      const smQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.SM_ONLY);
      this.smScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(smQuery);

      // DRAGON ADB config
      var config = JSON.parse(configData);

      var self = this;

      // Header
      // Application Name used in Branding Area
      self.appName = ko.observable("Basic Oracle Jet App for DRAGON");
      // User Info used in Global Navigation area
      self.userLogin = ko.observable("john.doe@oracle.com");

      self.numberOfDocuments = ko.observable();
      self.numberOfCollections = ko.observable();

      self.serviceURL = config.OJET_APP_SODA_API;

      self.Collections = ko.observable();

      self.Collection = Model.Model.extend({
        customURL: function (operation, collection, options) {
          var retObj = {};
          retObj['url'] = self.serviceURL + options['recordID'] + '?totalResults=true';
          retObj['headers'] = { "Authorization": "Basic " + btoa(config.OJET_APP_DATABASE_USER_NAME + ":" + config.OJET_APP_DATABASE_USER_PASSWORD) };
          return retObj;
        },
        idAttribute: 'name'
      });
      self.myCollection = new self.Collection();

      self.myCollections = Model.Collection.extend({
        customURL: function (operation, collection, options) {
          var retObj = {};
          retObj['url'] = self.serviceURL;
          retObj['headers'] = { "Authorization": "Basic " + btoa(config.OJET_APP_DATABASE_USER_NAME + ":" + config.OJET_APP_DATABASE_USER_PASSWORD) };
          return retObj;
        },
        model: self.myCollection,
        comparator: 'name'
      });
      self.Collections(new self.myCollections());
      self.Collections().fetch({
        success: function (response) {
          self.numberOfCollections(response.length);
          var totalDocuments = 0;
          for (var i = 0; i < response.length; i++) {
            response.at(i).fetch({
              success: function (data) {
                return data.attributes.totalResults;
              }
            }).then(function (data) {
              totalDocuments = totalDocuments + data.totalResults;
              self.numberOfDocuments(totalDocuments);
            })
          }
        }
      });

      // Footer
      self.footerLinks = [
        { name: 'About Oracle', linkId: 'aboutOracle', linkTarget: 'http://www.oracle.com/us/corporate/index.html#menu-about' },
        { name: "Contact Us", id: "contactUs", linkTarget: "http://www.oracle.com/us/corporate/contact/index.html" },
        { name: "Legal Notices", id: "legalNotices", linkTarget: "http://www.oracle.com/us/legal/index.html" },
        { name: "Terms Of Use", id: "termsOfUse", linkTarget: "http://www.oracle.com/us/legal/terms/index.html" },
        { name: "Your Privacy Rights", id: "yourPrivacyRights", linkTarget: "http://www.oracle.com/us/legal/privacy/index.html" },
      ];
    }

    return new ControllerViewModel();
  }
);
