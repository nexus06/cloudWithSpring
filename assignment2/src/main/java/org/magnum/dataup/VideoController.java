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

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);

    private VideoFileManager videoDataMgr;

    private Map<Long,Video> videos = new HashMap<Long, Video>();


    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v){
        return save(v);
    }


    /**
     * // Receives GET requests to /video and returns the current
     // list of videos in memory. Spring automatically converts
     // the list of videos to JSON because of the @ResponseBody
     // annotation.
     Returns the list of videos that have been added to the
     server as JSON. The list of videos does not have to be
     persisted across restarts of the server. The list of
     Video objects should be able to be unmarshalled by the
     client into a Collection<Video>.
     - The return content-type should be application/json, which
     will be the default if you use @ResponseBody
     * @return
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideoList(){
        return videos.values();
    }

    /**
     * Receives POST requests to VIDEO_DATA_PATH and returns VideoStatus
     * @param videoId
     * @param videoData as part
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH,method=RequestMethod.POST)
    public @ResponseBody
    VideoStatus uploadVideo(@PathVariable(VideoSvcApi.ID_PARAMETER) long videoId,
                                            @RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
                                            HttpServletResponse response) throws Exception {

        if(videos.get(videoId) != null){
            try {
                saveSomeVideo(videos.get(videoId),videoData);
                return new VideoStatus(VideoStatus.VideoState.READY);

            } catch (IOException e) {
                e.printStackTrace();
                response.sendError(HttpStatus.BAD_REQUEST.value());
                return new VideoStatus(VideoStatus.VideoState.READY);
            }
        }else{
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.sendError(HttpStatus.NOT_FOUND.value());
            return null;

        }
    }

    /**
     *Receives GET requests to VIDEO_DATA_PATH and returns response with videoData
     * @param videoId
     * @param response
     * @throws IOException
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH,method=RequestMethod.GET)
    public void getVideo(@PathVariable("id") long videoId,
                                                      HttpServletResponse response) throws IOException{

        if(videos.get(videoId)!=null){
            serveSomeVideo(videos.get(videoId), response);
        }else{
           response.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }

    /**
     * put video in cached map
     * @param entity
     * @return
     */
    public Video save(Video entity) {
        checkAndSetId(entity);
        videos.put(entity.getId(), entity);
        return entity;
    }

    /**
     * set id...
     * @param entity
     */
    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
            entity.setDataUrl(getDataUrl(entity.getId()));
        }
    }


    /**
     * save video in file system
     * @param v
     * @param videoData
     * @throws IOException
     */
    public void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
        if (videoDataMgr == null){
            videoDataMgr = VideoFileManager.get();
        }
        videoDataMgr.saveVideoData(v, videoData.getInputStream());

    }

    /**
     * serves video from file system
     * @param v
     * @param response
     * @throws IOException
     */
    public void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        if (videoDataMgr == null){
            videoDataMgr = VideoFileManager.get();
        }
        videoDataMgr.copyVideoData(v, response.getOutputStream());
    }

    /**
     * return formal url for a video
     * @param videoId
     * @return
     */
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    /**
     *
     * @return
     */
    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }
	
}
