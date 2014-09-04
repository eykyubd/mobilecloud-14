/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import retrofit.client.Response;

@Controller
public class VideoSvcController{
	
	// An in-memory list that the servlet uses to store the
	// videos that are sent to it by clients
	private List<Video> videos = new CopyOnWriteArrayList<Video>();
	
	public static final String VIDEO_SVC_PATH = "/video";
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	
	VideoFileManager videoFileMngr;
	
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos;
	}

	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(
			@RequestBody Video video
			)
	{
		//save to map to get 
		save(video);
		//generate dataUrl
		video.setDataUrl( getUrlBaseForLocalServer() + "/" + video.getId() + "/data");
		
		videos.add(video);
		
		return video;
	}

	/**
	 * Gets the video data
	 * @param  PathVar id
	 * @return 
	 * @throws IOException 
	 */
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
	public Response getData(
			@PathVariable long id,
			HttpServletResponse response
			) throws IOException  
	{
		videoFileMngr = VideoFileManager.get();
		
		try 
		{
			Video video = videosMap.get(id);
			if (videoFileMngr.hasVideoData(video)) 
			{
				videoFileMngr.copyVideoData(video, response.getOutputStream());
			}
			else
			{
				response.setStatus(404);
			}
		} 
		catch (NullPointerException e) 
		{
			response.setStatus(404);
		}
		
		return null;
	}
	
	/**
	 * Posts the video data to with its corresponding id
	 * @param id
	 * @param videoData
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id, 
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response
			) throws IOException 
	{	
		videoFileMngr = VideoFileManager.get();
		
		try 
		{
			Video video = videosMap.get(id);
			videoFileMngr.saveVideoData(video, videoData.getInputStream());
			return new VideoStatus(VideoState.READY);
		} 
		catch (NullPointerException e) 
		{
			response.setStatus(404);
		}

		return null;
	}
	
	
    private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videosMap = new HashMap<Long, Video>();

  	public Video save(Video entity) {
		checkAndSetId(entity);
		videosMap.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
	
}
