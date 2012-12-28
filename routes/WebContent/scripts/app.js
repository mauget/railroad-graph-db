
    var APP = APP || {};
	
	APP.drawMap = function() {
    	
        var mapOptions = {
          center: new google.maps.LatLng(39.0997, -94.5783),
          zoom: 4,
          mapTypeId: google.maps.MapTypeId.ROADMAP
        };
        APP.map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
	};
	
      
    $(document).ready(function(){
    	APP.drawMap();
    	
    	// Draw route1
    	$('#button1').click(function() {
    		var workUrl = 'http://myjavaneo4j.herokuapp.com/Controller?nodeA=' + $('#fromId').val() + '&nodeB=' + $('#toId').val();
    		APP.georssLayer = new google.maps.KmlLayer(workUrl);
    		APP.georssLayer.setMap(APP.map);
    		console.log(APP.georssLayer.getUrl());
    	});

    	// Clear -- redraw undorned map
    	$('#buttonReset').click(function() {
    		APP.drawMap();
    	});
    	
    });
