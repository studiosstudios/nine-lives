<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.1" name="objects" tilewidth="3072" tileheight="2048" tilecount="13" columns="0" objectalignment="topleft">
 <grid orientation="orthogonal" width="1" height="1"/>
 <tile id="0" type="Activator">
  <image width="1024" height="1024" source="button.png"/>
 </tile>
 <tile id="1">
  <image width="1024" height="1024" source="cat.png"/>
 </tile>
 <tile id="2" type="Checkpoint">
  <image width="1024" height="2048" source="checkpoint.png"/>
 </tile>
 <tile id="3" type="Flamethrower">
  <image width="1024" height="1024" source="flamethrower.png"/>
 </tile>
 <tile id="4" type="Laser">
  <image width="1024" height="1024" source="laser.png"/>
 </tile>
 <tile id="5" type="Mob">
  <image width="1024" height="2048" source="mob.png"/>
 </tile>
 <tile id="6" type="Spikes">
  <image width="1024" height="1024" source="spikes.png"/>
 </tile>
 <tile id="7">
  <image width="1024" height="2048" source="goal.png"/>
 </tile>
 <tile id="9" type="Spikes">
  <properties>
   <property name="type" propertytype="spikeType" value="left"/>
  </properties>
  <image width="1024" height="1024" source="spikes-left.png"/>
 </tile>
 <tile id="10" type="Spikes">
  <properties>
   <property name="type" propertytype="spikeType" value="right"/>
  </properties>
  <image width="1024" height="1024" source="spikes-right.png"/>
 </tile>
 <tile id="11" type="Spikes">
  <properties>
   <property name="type" propertytype="spikeType" value="center"/>
  </properties>
  <image width="1024" height="1024" source="spikes-center.png"/>
 </tile>
 <tile id="12">
  <image width="3072" height="1024" source="ceiling-light.png"/>
 </tile>
 <tile id="13">
  <image width="1024" height="1024" source="wall-light.png"/>
 </tile>
</tileset>
