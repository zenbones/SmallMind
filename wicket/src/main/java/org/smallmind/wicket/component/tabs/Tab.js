if (typeof SMALLMIND == "undefined") {
   var SMALLMIND = {};
}

if (typeof SMALLMIND.component == "undefined") {
   SMALLMIND.component = {};
}

SMALLMIND.component.tab = new function() {

   this.highlight = function(tab) {

      if (tab.getAttribute("selected") == null) {
         tab.className = "tabhighlighted"
      }
   }

   this.normal = function(tab) {

      if (tab.getAttribute("selected") == null) {
         tab.className = "tabstandard"
      }
   }
}