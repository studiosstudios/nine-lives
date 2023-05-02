<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.1" name="objects" tilewidth="40" tileheight="64" tilecount="6" columns="0" objectalignment="topleft">
 <grid orientation="orthogonal" width="1" height="1"/>
 <tile id="0">
  <properties>
   <property name="id" value="button1"/>
   <property name="type" propertytype="activators" value="button"/>
  </properties>
  <image width="32" height="32" source="objects/button.png"/>
 </tile>
 <tile id="3">
  <image width="40" height="34" source="objects/cat.png"/>
 </tile>
 <tile id="4">
  <image width="32" height="64" source="objects/checkpoint.png"/>
 </tile>
 <tile id="5">
  <properties>
   <property name="activatorID" value=""/>
   <property name="active" type="bool" value="true"/>
  </properties>
  <image width="32" height="32" source="objects/spikes.png"/>
 </tile>
 <tile id="6">
  <properties>
   <property name="activatorID" value=""/>
   <property name="active" type="bool" value="true"/>
   <property name="pushable" type="bool" value="false"/>
  </properties>
  <image width="32" height="32" source="objects/flamethrower.png"/>
 </tile>
 <tile id="9">
  <properties>
   <property name="activatorID" value="button1"/>
   <property name="active" type="bool" value="true"/>
  </properties>
  <image width="32" height="32" source="objects/laser-scaled.png"/>
 </tile>
</tileset>
