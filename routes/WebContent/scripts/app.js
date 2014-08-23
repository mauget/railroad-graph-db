
    var APP = APP || {};

    // http://myjavaneo4j.herokuapp.com/Controller?nodeA=68214&nodeB=40126 
    APP.urlStem = 'http://myjavaneo4j.herokuapp.com';
    //APP.urlStem = 'http://localhost:8080/routes';
	
	APP.drawMap = function() {
    	
        var mapOptions = {
          center: new google.maps.LatLng(39.0997, -94.5783),
          zoom: 4,
          mapTypeId: google.maps.MapTypeId.ROADMAP
        };
        APP.map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
	};
	
	APP.initDynSize = function() {
		resizeMapHeight();

		window.onresize = function(event) {
			resizeMapHeight();
		};

		function resizeMapHeight() {
			var pageHeight   = $(window).height();
			var headerHeight = $('#map_header').outerHeight(true);
			var footerHeight = $('#map_footer').outerHeight(true);
			var vph = String(Math.max(pageHeight - headerHeight - footerHeight, 0));
			
			$('#map_canvas').css( {'height': vph + 'px'}) ;
		};
	};
      
    $(document).ready(function() {
    	APP.initDynSize();
    	
    	APP.drawMap();
    	
    	// Draw route1
    	$('#buttonRoute').click(function() {
    		// Would rather use a template here, but we does the best with what we have  ..
    		var finalUrl = APP.urlStem+'/Controller?nodeA='+$('#fromId').val()+'&nodeB='+$('#toId').val();
    		
    		APP.georssLayer = new google.maps.KmlLayer(finalUrl);
    		APP.georssLayer.setMap(APP.map);
    		
    		console.log(APP.georssLayer.getUrl());
    	});

    	// Clear -- redraw unadorned map
    	$('#buttonReset').click(function() {
    		APP.drawMap();
    	});
    	
    });
