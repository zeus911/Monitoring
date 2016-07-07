/**
Copyright 2016 ATOS SPAIN S.A.

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Authors Contact:
Francisco Javier Nieto. Atos Research and Innovation, Atos SPAIN SA
@email francisco.nieto@atos.net
**/

package org.indigo.occiprobe.openstack;

public class InspectVMResult 
{
	private int inspectVMAvailability;
	private int inspectVMResult;
	private long inspectVMResponseTime;
	
	public InspectVMResult (int availability, int result, long responseTime)
	{
		inspectVMAvailability = availability;
		inspectVMResult = result;
		inspectVMResponseTime = responseTime;
	}
	
	public int getInspectVMAvailability ()
	{
		return inspectVMAvailability;
	}
	
	public int getInspectVMResult ()
	{
		return inspectVMResult;
	}
	
	public long getInspectVMResponseTime ()
	{
		return inspectVMResponseTime;
	}
}