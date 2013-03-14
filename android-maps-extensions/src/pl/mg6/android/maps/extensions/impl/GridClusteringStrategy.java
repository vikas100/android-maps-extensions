/*
 * Copyright (C) 2013 Maciej G�rski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.mg6.android.maps.extensions.impl;

import java.util.List;

import android.util.SparseArray;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

class GridClusteringStrategy implements ClusteringStrategy {

	private GoogleMap provider;
	private float zoom;
	private List<DelegatingMarker> markers;

	private SparseArray<ClusterMarker> clusters;

	public GridClusteringStrategy(GoogleMap provider, List<DelegatingMarker> markers) {
		this.provider = provider;
		this.markers = markers;
		this.zoom = provider.getCameraPosition().zoom;
		recalculate();
	}
	
	@Override
	public void cleanup() {
		if (clusters != null) {
			for (int i = 0; i < clusters.size(); i++) {
				ClusterMarker cluster = clusters.valueAt(i);
				cluster.cleanup();
			}
			clusters = null;
		}
		for (DelegatingMarker marker : markers) {
			if (marker.isVisible()) {
				marker.changeVisible(true);
			}
		}
	}

	@Override
	public void onZoomChange(float zoom) {
		this.zoom = zoom;
		recalculate();
	}

	@Override
	public void onAdd(DelegatingMarker marker) {
		markers.add(marker);
		recalculate();
	}

	@Override
	public void onRemove(DelegatingMarker marker) {
		markers.remove(marker);
		recalculate();
	}

	@Override
	public void onPositionChange(DelegatingMarker marker) {
		recalculate();
	}

	@Override
	public void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible) {
		marker.changeVisible(visible);
		recalculate();
	}

	private void recalculate() {
		if (clusters != null) {
			for (int i = 0; i < clusters.size(); i++) {
				ClusterMarker cluster = clusters.valueAt(i);
				cluster.cleanup();
			}
			clusters = null;
		}
		if (zoom > 4.0f) {
			for (DelegatingMarker marker : markers) {
				if (marker.isVisible()) {
					marker.changeVisible(true);
				}
			}
		} else {
			clusters = new SparseArray<ClusterMarker>();
			for (DelegatingMarker marker : markers) {
				LatLng position = marker.getPosition();
				int clusterId = calculateClusterId(position);
				ClusterMarker cluster = clusters.get(clusterId);
				if (cluster == null) {
					cluster = new ClusterMarker(provider.addMarker(new MarkerOptions().position(position).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))));
					clusters.put(clusterId, cluster);
				}
				cluster.add(marker);
			}
			for (int i = 0; i < clusters.size(); i++) {
				ClusterMarker cluster = clusters.valueAt(i);
				cluster.fixVisibility();
			}
		}
	}
	
	private int calculateClusterId(LatLng position) {
		int y = (int) (position.latitude + 180.0) / 180;
		int x = (int) (position.longitude + 90.0) / 90;
		return 2 * x + y;
	}
}