if (typeof SMALLMIND == "undefined") {
   var SMALLMIND = {};
}

if (typeof SMALLMIND.component == "undefined") {
   SMALLMIND.component = {};
}

SMALLMIND.component.button = new function() {

   this.submit = function (submitFormId) {

      var submitForm = document.getElementById(submitFormId);

      submitForm.submit();
   }

   this.normal = function(button) {

      if (button.getAttribute("incapacitated") == "false") {
         button.className = "buttonstandard"
      }
   }

   this.highlight = function(button) {

      if (button.getAttribute("incapacitated") == "false") {
         button.className = "buttonhighlighted"
      }
   }

   this.press = function(button) {

      if (button.getAttribute("incapacitated") == "false") {
         button.className = "buttonpressed"
      }
   }

   this.isEnabled = function(buttonId) {

      var button = document.getElementById(buttonId);

      return button.getAttribute("incapacitated") != "true";
   }

   this.disable = function(buttonId) {

      var button = document.getElementById(buttonId);

      button.setAttribute("incapacitated", "true");
      button.className = "buttonincapacitated";
   }

   this.enable = function(buttonId) {

      var button = document.getElementById(buttonId);

      button.setAttribute("incapacitated", "false");
      button.className = "buttonstandard";
   }
}