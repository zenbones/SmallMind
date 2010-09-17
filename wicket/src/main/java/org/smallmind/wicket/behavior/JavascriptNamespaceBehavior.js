/// Create the Namespace Manager that we'll use
///to make creating namespaces a little easier.
if (typeof SMALLMIND == 'undefined') var SMALLMIND {};
if (!SMALLMIND.namespace) SMALLMIND.namespace = {};

SMALLMIND.namespace.manager = {
   register: function(ns) {
      if (ns.length > 0) {
         myBaseNs = ns.substring(0, ns.lastIndexOf('.'));
         this.Register(myBaseNs);
         eval("if(!window." + ns + ") window." + ns + " ={};");
      }
   }
};