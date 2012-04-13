SMALLMIND.visualization.flot.compareArray = function (array1, array2) {

  var arrayIndex;

  if ((array1 == null) && (array2 == null)) {
    return true;
  }

  if ((array1 == null) || (array2 == null)) {
    return false;
  }

  if (array1.length == array2.length) {
    for (arrayIndex in array1) {
      if (array1[arrayIndex] != array2[arrayIndex]) {
        return false;
      }
    }

    return true;
  }

  return false;
};

SMALLMIND.visualization.flot.findPos = function (obj) {

  var curleft = 0;
  var curtop = 0;

  if (obj.offsetParent) {
    do {
      curleft += obj.offsetLeft;
      curtop += obj.offsetTop;

    } while (obj = obj.offsetParent);

    return [curleft, curtop];
  }
};

SMALLMIND.visualization.flot.showTooltip = function (id, x, y, contents) {

  jQuery("<div id='" + id + "'>" + contents + "</div>").css({
    position:'absolute',
    display:'none',
    'font-size':'2em',
    top:y - 10,
    left:x,
    border:'1px solid #fdd',
    padding:'2px',
    'background-color':'#fee',
    opacity:0.9
  }).appendTo("body").fadeIn(200);
};

// Flot class constructor.
SMALLMIND.visualization.flot.Flot = function (container) {

  this.container = container;
};

// Flot.draw() method.
SMALLMIND.visualization.flot.Flot.prototype.draw = function (data, options) {

  var flotDiv;
  var titleDiv = null;

  this.container.style.width = options.width;
  this.container.style.height = options.height;

  if ((flotDiv = document.getElementById("SMALLMIND.visualization.flot_div." + this.container.id)) != null) {
    this.container.removeChild(flotDiv);
  }

  flotDiv = document.createElement("div");
  flotDiv.setAttribute("id", "SMALLMIND.visualization.flot_div." + this.container.id);
  flotDiv.style.width = "100%";
  flotDiv.style.height = "100%";
  if (options.fontSize != null) {
    flotDiv.style.fontSize = options.fontSize;
  }
  else {
    flotDiv.style.fontSize = "11px";
  }

  this.container.appendChild(flotDiv);

  if (options.title != null) {

    titleDiv = document.createElement("div");
    titleDiv.style.width = "100%";
    titleDiv.style.fontWeight = "bold";
    titleDiv.style.textAlign = "center";
    titleDiv.style.paddingBottom = "5px";

    if (options.titleFontSize != null) {
      titleDiv.style.fontSize = options.titleFontSize;
    }
    if (options.titleColor != null) {
      titleDiv.style.color = titleColor;
    }

    titleDiv.appendChild(document.createTextNode(options.title));
    flotDiv.appendChild(titleDiv);
  }

  if (data.getNumberOfRows() == 0) {

    var emptyDiv;

    emptyDiv = document.createElement("div");
    emptyDiv.style.width = "100%";
    emptyDiv.style.height = "100%";
    emptyDiv.style.verticalAlign = "middle";
    emptyDiv.style.textAlign = "center";
    emptyDiv.appendChild(document.createTextNode("No data"));

    flotDiv.appendChild(emptyDiv);
  }
  else {

    if (data.getNumberOfColumns() == 1) {
      alert('Error in query: at least 2 data columns are required');
      return;
    }

    fixTextAxis(options.xaxis);
    fixTextAxis(options.x2axis);
    fixTextAxis(options.yaxis);
    fixTextAxis(options.y2axis);

    var flotData;
    var colIndex;
    var rowIndex;
    var series;
    var xValue;
    var yValue;

    flotData = new Array();
    for (colIndex = 1; colIndex < data.getNumberOfColumns(); colIndex++) {
      if ((series = data.getColumnProperties(colIndex)) == null) {
        series = new Object();
      }

      series.data = new Array();

      for (rowIndex = 0; rowIndex < data.getNumberOfRows(); rowIndex++) {
        if (options.inverted) {
          if (((xValue = data.getValue(rowIndex, colIndex)) != null) && ((yValue = data.getValue(rowIndex, 0)) != null)) {
            series.data[rowIndex] = [fixTextValue(options, series, 'x', xValue), fixTextValue(options, series, 'y', yValue)];
          }
        }
        else {
          if (((xValue = data.getValue(rowIndex, 0)) != null) && ((yValue = data.getValue(rowIndex, colIndex)) != null)) {
            series.data[rowIndex] = [fixTextValue(options, series, 'x', xValue), fixTextValue(options, series, 'y', yValue)];
          }
        }
      }

      flotData.push(series);
    }

    var graphDiv;
    var graphTable;
    var middleTable;
    var graphTBody;
    var middleTBody;
    var topTr;
    var middleTr;
    var bottomTr;
    var singleTr;
    var topTd;
    var middleTd;
    var bottomTd;
    var leftTd;
    var graphTd;
    var rightTd;

    graphTable = document.createElement("table");
    graphTable.setAttribute("cellpadding", 0);
    graphTable.setAttribute("cellspacing", 0);
    graphTable.setAttribute("border", 0);
    graphTable.setAttribute("width", this.container.offsetWidth);
    graphTable.setAttribute("height", (titleDiv == null) ? this.container.offsetHeight : this.container.offsetHeight - titleDiv.offsetHeight);
    flotDiv.appendChild(graphTable);

    graphTBody = document.createElement("tbody");
    graphTable.appendChild(graphTBody);

    topTr = document.createElement("tr");
    graphTBody.appendChild(topTr);
    middleTr = document.createElement("tr");
    graphTBody.appendChild(middleTr);
    bottomTr = document.createElement("tr");
    graphTBody.appendChild(bottomTr);

    topTd = document.createElement("td");
    topTr.appendChild(topTd);

    middleTd = document.createElement("td");
    middleTd.setAttribute("width", "100%");
    middleTd.setAttribute("height", "100%");

    middleTr.appendChild(middleTd);
    bottomTd = document.createElement("td");
    bottomTr.appendChild(bottomTd);

    middleTable = document.createElement("table");
    middleTable.setAttribute("cellpadding", 0);
    middleTable.setAttribute("cellspacing", 0);
    middleTable.setAttribute("border", 0);
    middleTable.setAttribute("width", "100%");
    middleTable.setAttribute("height", "100%");
    middleTd.appendChild(middleTable);

    middleTBody = document.createElement("tbody");
    middleTable.appendChild(middleTBody);

    singleTr = document.createElement("tr");
    middleTBody.appendChild(singleTr);

    leftTd = document.createElement("td");
    singleTr.appendChild(leftTd);
    graphTd = document.createElement("td");
    graphTd.setAttribute("width", "100%");
    graphTd.setAttribute("height", "100%");
    singleTr.appendChild(graphTd);
    rightTd = document.createElement("td");
    singleTr.appendChild(rightTd);

    if ((options.xaxis != null) && (options.xaxis.title != null)) {
      bottomTd.style.textAlign = "center";
      bottomTd.style.paddingTop = "3px";
      bottomTd.style.paddingBottom = "3px";

      if (options.fontSize != null) {
        bottomTd.style.fontSize = options.fontSize;
      }
      else {
        bottomTd.style.fontSize = "11px";
      }
      bottomTd.appendChild(document.createTextNode(options.xaxis.title));
    }

    if ((options.x2axis != null) && (options.x2axis.title != null)) {
      topTd.style.textAlign = "center";
      topTd.style.paddingTop = "3px";
      topTd.style.paddingBottom = "3px";

      if (options.fontSize != null) {
        topTd.style.fontSize = options.fontSize;
      }
      else {
        topTd.style.fontSize = "11px";
      }
      topTd.appendChild(document.createTextNode(options.x2axis.title));
    }

    if ((options.yaxis != null) && (options.yaxis.title != null)) {

      var leftTextDiv;
      var leftFontSize;

      leftFontSize = (options.fontSize != null) ? options.fontSize : '11px';

      leftTd.style.paddingLeft = "3px";
      leftTd.style.paddingRight = "8px";

      leftTextDiv = document.createElement("div");
      leftTextDiv.style.height = leftTd.offsetHeight;
      leftTextDiv.style.width = 15;

      if (navigator.userAgent.indexOf("MSIE") >= 0) {
        leftTextDiv.style.writingMode = "tb-rl";
        leftTextDiv.style.fontSize = leftFontSize;
        leftTextDiv.style.fontFamily = "arial, helvetica";
        leftTextDiv.style.textAlign = "center";
        leftTd.appendChild(leftTextDiv);
        leftTextDiv.appendChild(document.createTextNode(options.yaxis.title));
      }
      else {
        leftTd.appendChild(leftTextDiv);

        var leftSvgObject = document.createElement("object");

        leftSvgObject.setAttribute("type", "image/svg+xml");
        leftSvgObject.setAttribute("data", "data:image/svg+xml," + "<svg xmlns='http://www.w3.org/2000/svg'><g transform='translate(" + ((leftTd.offsetWidth / 2) - 3) + "," + (leftTd.offsetHeight / 2) + ")'><text x='0' y='0' text-anchor='middle' transform='rotate(-90)' font-size='" + leftFontSize + "' font-family='arial, helvetica'>" + options.yaxis.title + "</text></g></svg>");
        leftTextDiv.appendChild(leftSvgObject);
      }
    }

    if ((options.y2axis != null) && (options.y2axis.title != null)) {

      var rightTextDiv;
      var rightFontSize;

      rightFontSize = (options.fontSize != null) ? options.fontSize : '11px';

      rightTd.style.paddingLeft = "8px";
      rightTd.style.paddingRight = "3px";

      rightTextDiv = document.createElement("div");
      rightTextDiv.style.height = rightTd.offsetHeight;
      rightTextDiv.style.width = 15;

      if (navigator.userAgent.indexOf("MSIE") >= 0) {
        rightTextDiv.style.writingMode = "tb-rl";
        rightTextDiv.style.fontSize = leftFontSize;
        rightTextDiv.style.fontFamily = "arial, helvetica";
        rightTextDiv.style.textAlign = "center";
        rightTd.appendChild(rightTextDiv);
        rightTextDiv.appendChild(document.createTextNode(options.y2axis.title));
      }
      else {
        rightTd.appendChild(rightTextDiv);

        var rightSvgObject = document.createElement("object");

        rightSvgObject.setAttribute("type", "image/svg+xml");
        rightSvgObject.setAttribute("data", "data:image/svg+xml," + "<svg xmlns='http://www.w3.org/2000/svg'><g transform='translate(" + ((rightTd.style.width / 2) + 4) + "," + (rightTd.offsetHeight / 2) + ")'><text x='0' y='0' text-anchor='middle' transform='rotate(90)' font-size='" + rightFontSize + "' font-family='arial, helvetica'>" + options.y2axis.title + "</text></g></svg>");
        rightTextDiv.appendChild(rightSvgObject);
      }
    }

    graphDiv = document.createElement("div");
    graphDiv.style.width = graphTable.offsetWidth - leftTd.offsetWidth - rightTd.offsetWidth;
    graphDiv.style.height = graphTable.offsetHeight - bottomTd.offsetHeight - topTd.offsetHeight;
    if (options.fontSize != null) {
      graphDiv.style.fontSize = options.fontSize;
    }
    else {
      graphDiv.style.fontSize = "11px";
    }

    graphTd.appendChild(graphDiv);

    this.plot = jQuery.plot(graphDiv, flotData, options);

    if (options.hover != null) {
      jQuery('#' + this.container.id).bind("plothover", function (event, pos, item) {
        options.hover(event, pos, item);
      });
    }

    if (options.click != null) {
      jQuery('#' + this.container.id).bind("plotclick", function (event, pos, item) {
        options.click(event, pos, item);
      });
    }
  }

  function fixTextAxis (axis) {

    if ((axis != null) && (axis.textConversion != null)) {

      var textTicks;
      var tickIndex;

      textTicks = new Array();
      for (tickIndex in axis.textConversion) {
        textTicks[tickIndex] = [tickIndex, (axis.tickFormatter == null) ? axis.textConversion[tickIndex] : axis.tickFormatter(axis.textConversion[tickIndex])];
      }

      axis.ticks = textTicks;
    }
  }

  function fixTextValue (options, series, axisType, value) {

    var axis;

    if (axisType == "x") {
      axis = (series.xaxis == "2") ? options.x2axis : options.xaxis;
    }
    else {
      axis = (series.yaxis == "2") ? options.y2axis : options.yaxis;
    }

    if ((axis != null) && (axis.textConversion != null)) {

      var tickIndex;

      for (tickIndex in axis.textConversion) {
        if (value == axis.textConversion[tickIndex]) {

          return tickIndex;
        }
      }

      return null;
    }
    else {

      return value;
    }
  }
};