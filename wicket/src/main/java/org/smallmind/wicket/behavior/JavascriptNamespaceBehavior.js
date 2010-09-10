/// Create the Namespace Manager that we'll use
///to make creating namespaces a little easier.
if (typeof Namespace == 'undefined') var Namespace = {};
if (!Namespace.Manager) Namespace.Manager = {};

Namespace.Manager = {
   Register: function(ns) {
      if (ns.length > 0) {
         myBaseNs = ns.substring(0, ns.lastIndexOf('.'));
         this.Register(myBaseNs);
         eval("if(!window." + ns + ") window." + ns + " ={};");
      }
   }
};