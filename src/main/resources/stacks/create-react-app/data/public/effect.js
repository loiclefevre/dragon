let mEffect;
let mGLContext;
let mRenderer;
let caps;
let mIs20;
let mShaderTextureLOD;
let mCreated = false;
let mProgramCopy;
let mProgramDownscale;
let mMaxBuffers = 4;
let mBuffers = [];
let canvas;
let mXres;
let mYres;
let mIsLowEnd;
let mInputs;
let mProgram;
let mFrame = 0;
let mMousePosX = 0;
let mMousePosY = 0;
let mMouseOriX = 0;
let mMouseOriY = 0;
let mFPSDiv;

function startEffect() {
    mIsLowEnd = ( navigator.userAgent.match(/Android/i) || 
                       navigator.userAgent.match(/webOS/i) || 
                       navigator.userAgent.match(/iPhone/i) || 
                       navigator.userAgent.match(/iPad/i) || 
                       navigator.userAgent.match(/iPod/i) || 
                       navigator.userAgent.match(/BlackBerry/i) || 
                       navigator.userAgent.match(/Windows Phone/i) ) ? true : false;

    mInputs = [null, null, null, null];

    piDisableTouch();

    mGLContext = piCreateGlContext(canvas = document.getElementById("background"), false, false, false, true);
    if( mGLContext == null ) return;

    document.getElementById("root").onmousemove = function(ev)
    {
        var rect = canvas.getBoundingClientRect();
        mMouseOriX = mMousePosX = Math.floor((ev.clientX-rect.left)/(rect.right-rect.left)*canvas.width);
        mMouseOriY = mMousePosY = Math.floor(canvas.height - (ev.clientY-rect.top)/(rect.bottom-rect.top)*canvas.height);
    }

    mFPSDiv = document.getElementById("fps");

    mXres = canvas.width;
    mYres = canvas.height;

    mRenderer = piRenderer();

    if (!mRenderer.Initialize(mGLContext)) return;

    caps = mRenderer.GetCaps();
    mIs20 = caps.mIsGL20;
    mShaderTextureLOD = caps.mShaderTextureLOD;

    var vsSource;
    if( mIs20 )
        vsSource = "layout(location = 0) in vec2 pos; void main() { gl_Position = vec4(pos.xy,0.0,1.0); }";
    else
        vsSource = "attribute vec2 pos; void main() { gl_Position = vec4(pos.xy,0.0,1.0); }";

    MakeHeader();

    var fsSource = 
    mHeader +
    "\n" +
    "// Created by inigo quilez - iq/2013\n" +
    "// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.\n" +
    "\n" +
    "float noise( in vec3 x )\n" +
    "{\n" +
    "    vec3 p = floor(x);\n" +
    "    vec3 f = fract(x);\n" +
    "    f = f*f*(3.0-2.0*f);\n" +
    "\n" +
    "    vec2 uv = (p.xy+vec2(37.0,17.0)*p.z) + f.xy;\n" +
    "    vec2 rg = textureLod( iChannel0, (uv+ 0.5)/256.0, 0.0 ).yx;\n" +
    "    return mix( rg.x, rg.y, f.z );\n" +
    "}\n" +
    "\n" +
    "vec4 map( vec3 p )\n" +
    "{\n" +
    "    float den = 0.2 - p.y;\n" +
    "\n" +
    "   // invert space\n" +
    "    p = -7.0*p/dot(p,p);\n" +
    "\n" +
    "   // twist space\n" +
    "    float co = cos(den - 0.25*iTime);\n" +
    "    float si = sin(den - 0.25*iTime);\n" +
    "    p.xz = mat2(co,-si,si,co)*p.xz;\n" +
    "\n" +
    "   // smoke\n" +
    "    float f;\n" +
    "    vec3 q = p                          - vec3(0.0,1.0,0.0)*iTime;\n" +
    "    f  = 0.50000*noise( q ); q = q*2.02 - vec3(0.0,1.0,0.0)*iTime;\n" +
    "    f += 0.25000*noise( q ); q = q*2.03 - vec3(0.0,1.0,0.0)*iTime;\n" +
    "    f += 0.12500*noise( q ); q = q*2.01 - vec3(0.0,1.0,0.0)*iTime;\n" +
    "    f += 0.06250*noise( q ); q = q*2.02 - vec3(0.0,1.0,0.0)*iTime;\n" +
    "    f += 0.03125*noise( q );\n" +
    "\n" +
    "    den = clamp( den + 4.0*f, 0.0, 1.0 );\n" +
    "\n" +    
    "    vec3 col = mix( vec3(1.0,0.9,0.8), vec3(0.4,0.15,0.1), den ) + 0.05*sin(p);\n" +
    "\n" +    
    "    return vec4( col, den );\n" +
    "}\n" +
    "\n" +
    "vec3 raymarch( in vec3 ro, in vec3 rd, in vec2 pixel )\n" +
    "{\n" +
    "    vec4 sum = vec4( 0.0 );\n" +
    "\n" +
    "    float t = 0.0;\n" +
    "\n" +
    "    // dithering\n" +
    "    t += 0.05*textureLod( iChannel0, pixel.xy/iChannelResolution[0].x, 0.0 ).x;\n" +
    "\n" +    
    "    for( int i=0; i<100; i++ )\n" +
    "    {\n" +
    "        if( sum.a > 0.99 ) break;\n" +
    "\n" +        
    "        vec3 pos = ro + t*rd;\n" +
    "        vec4 col = map( pos );\n" +
    "\n" +
    "        col.xyz *= mix( 3.1*vec3(1.0,0.7,0.05), vec3(0.8,0.53,0.5), clamp( (pos.y-0.2)/2.0, 0.0, 1.0 ) );\n" +
    "\n" +
    "        col.a *= 0.4;\n" +
    "        col.rgb *= col.a;\n" +
    "\n" +
    "        sum = sum + col*(1.0 - sum.a);\n" +
    "\n" +
    "        t += 0.05;\n" +
    "    }\n" +
    "\n" +
    "    return clamp( sum.xyz, 0.0, 1.0 );\n" +
    "}\n" +
    "\n" +
    "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
    "{\n" +
    "    vec2 q = fragCoord.xy / iResolution.xy;\n" +
    "    vec2 p = -1.0 + 2.0*q;\n" +
    "    p.x *= iResolution.x/ iResolution.y;\n" +
    "\n" +    
    "    vec2 mo = iMouse.xy / iResolution.xy;\n" +
    "    if( iMouse.w<=0.00001 ) mo=vec2(0.0);\n" +
    "\n" +    
    "    // camera\n" +
    "    vec3 ro = 4.0*normalize(vec3(cos(3.0*mo.x), 1.4 - 1.0*(mo.y-.1), sin(3.0*mo.x)));\n" +
    "    vec3 ta = vec3(0.0, 1.0, 0.0);\n" +
    "    float cr = 0.5*cos(0.7*iTime);\n" +
    "\n" +    
    "    // shake\n" +
    "    //ro += 0.1*(-1.0+2.0*textureLod( iChannel0, iTime*vec2(0.010,0.014), 0.0 ).xyz);\n" +
    "    //ta += 0.1*(-1.0+2.0*textureLod( iChannel0, iTime*vec2(0.013,0.008), 0.0 ).xyz);\n" +
    "\n" +    
    "    // build ray\n" +
    "    vec3 ww = normalize( ta - ro);\n" +
    "    vec3 uu = normalize(cross( vec3(sin(cr),cos(cr),0.0), ww ));\n" +
    "    vec3 vv = normalize(cross(ww,uu));\n" +
    "    vec3 rd = normalize( p.x*uu + p.y*vv + 2.0*ww );\n" +
    "\n" +    
    "    // raymarch\n" +
    "    vec3 col = raymarch( ro, rd, fragCoord );\n" +
    "\n" +
    "    // contrast and vignetting\n" +
    "    col = col*0.5 + 0.5*col*col*(3.0-2.0*col);\n" +
    "    col *= 0.25 + 0.75*pow( 16.0*q.x*q.y*(1.0-q.x)*(1.0-q.y), 0.1 );\n" +
    "\n" +    
    "    fragColor = vec4( col, 1.0 );\n" +
    "}"
    + mImagePassFooter;

    var res = mRenderer.CreateShader(vsSource, fsSource);
    if (res.mResult == false) {
        console.log( res.mInfo );
        return;
    }

    mProgram = res;

    loadTextures({ mType: "texture", mID: "", mSrc: "./noise.png", mSampler: {filter : "linear"}, mPreviewSrc: "" });

    mCreated = true;

    window.setTimeout(function()
    {
        startRendering();

    }.bind(this), 10 );
}

let fpsInterval = 1000.0 / 60.0;
let mTo = getRealTime();
let mTf = 0;
let mFPS = piCreateFPSCounter();

function startRendering()
{
    mFPS.Reset(mTo);

    function renderLoop2()
    {
        requestAnimFrame(renderLoop2);

        var time = getRealTime();

        var ltime = 0.0;
        var dtime = 0.0;
            ltime = time - mTo;
            dtime = ltime - mTf; 
            mTf = ltime;
       
        var newFPS = mFPS.Count( time );

        mRenderer.SetRenderTarget( null );
        Paint(ltime/1000.0, dtime/1000.0, mFPS.GetFPS(), mMouseOriX, mMouseOriY, mMousePosX, mMousePosY, mXres, mYres );
        mFrame++;

        if( mFPSDiv && newFPS )
        {
            mFPSDiv.textContent = mFPS.GetFPS().toFixed(1) + " fps";
        }
    }

    renderLoop2();
}

function Paint( time, dtime, fps, mouseOriX, mouseOriY, mousePosX, mousePosY, xres, yres )
{
    var times = [ 0.0, 0.0, 0.0, 0.0 ];

    let d = new Date();

    var dates = [ d.getFullYear(), // the year (four digits)
                  d.getMonth(),	   // the month (from 0-11)
                  d.getDate(),     // the day of the month (from 1-31)
                  d.getHours()*60.0*60 + d.getMinutes()*60 + d.getSeconds()  + d.getMilliseconds()/1000.0 ];

    var mouse = [  mousePosX, mousePosY, mouseOriX, mouseOriY ];


    //------------------------
    
    var resos = [ 0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0 ];
    let texIsLoaded = [0, 0, 0, 0 ];
    var texID = [ null, null, null, null];

    for( var i=0; i<mInputs.length; i++ )
    {
        var inp = mInputs[i];

        if( inp==null )
        {
        }
        else if( inp.mInfo.mType=="texture" )
        {
            if( inp.loaded==true  )
            {
                texID[i] = inp.globject;
                texIsLoaded[i] = 1;
                resos[3*i+0] = inp.image.width;
                resos[3*i+1] = inp.image.height;
                resos[3*i+2] = 1;
            }
        }
    }

    mRenderer.AttachTextures( 4, texID[0], texID[1], texID[2], texID[3] );

    //-----------------------------------
    var prog = mProgram;

    mRenderer.AttachShader(prog);

    mRenderer.SetShaderConstant1F(  "iTime", time);
    mRenderer.SetShaderConstant3F(  "iResolution", xres, yres, 1.0);
    mRenderer.SetShaderConstant4FV( "iMouse", mouse);
    mRenderer.SetShaderConstant1FV( "iChannelTime", times );              // OBSOLETE
    mRenderer.SetShaderConstant4FV( "iDate", dates );
    mRenderer.SetShaderConstant3FV( "iChannelResolution", resos );        // OBSOLETE
    mRenderer.SetShaderTextureUnit( "iChannel0", 0 );
    mRenderer.SetShaderTextureUnit( "iChannel1", 1 );
    mRenderer.SetShaderTextureUnit( "iChannel2", 2 );
    mRenderer.SetShaderTextureUnit( "iChannel3", 3 );
    mRenderer.SetShaderConstant1I(  "iFrame", mFrame );
    mRenderer.SetShaderConstant1F(  "iTimeDelta", dtime);
    mRenderer.SetShaderConstant1F(  "iFrameRate", fps );

    mRenderer.SetShaderConstant1F(  "iCh0.time", times[0] );
    mRenderer.SetShaderConstant1F(  "iCh1.time", times[1] );
    mRenderer.SetShaderConstant1F(  "iCh2.time", times[2] );
    mRenderer.SetShaderConstant1F(  "iCh3.time", times[3] );
    mRenderer.SetShaderConstant3F(  "iCh0.size", resos[0], resos[ 1], resos[ 2] );
    mRenderer.SetShaderConstant3F(  "iCh1.size", resos[3], resos[ 4], resos[ 5] );
    mRenderer.SetShaderConstant3F(  "iCh2.size", resos[6], resos[ 7], resos[ 8] );
    mRenderer.SetShaderConstant3F(  "iCh3.size", resos[9], resos[10], resos[11] );
    mRenderer.SetShaderConstant1I(  "iCh0.loaded",       texIsLoaded[0] );
    mRenderer.SetShaderConstant1I(  "iCh1.loaded",       texIsLoaded[1] );
    mRenderer.SetShaderConstant1I(  "iCh2.loaded",       texIsLoaded[2] );
    mRenderer.SetShaderConstant1I(  "iCh3.loaded",       texIsLoaded[3] );

    var l1 = mRenderer.GetAttribLocation(mProgram, "pos");

    mRenderer.SetViewport([0, 0, xres, yres]);
    mRenderer.DrawFullScreenTriangle_XY( l1 );

    mRenderer.DettachTextures();
}

let mHeader;
let mImagePassFooter;

function MakeHeader()
{
    var header = "";

    header += "#define HW_PERFORMANCE " + ((mIsLowEnd==true)?"0":"1") + "\n";

    header += "uniform vec3      iResolution;\n" +
              "uniform float     iTime;\n" +
              "uniform float     iChannelTime[4];\n" +
              "uniform vec4      iMouse;\n" +
              "uniform vec4      iDate;\n" +
              "uniform vec3      iChannelResolution[4];\n" +
              "uniform int       iFrame;\n" +
              "uniform float     iTimeDelta;\n" +
              "uniform float     iFrameRate;\n";

    for( let i=0; i<mInputs.length; i++ )
    {
        let inp = mInputs[i];

        // old API
             if( inp==null )                  header += "uniform sampler2D iChannel" + i + ";\n";
        else if( inp.mInfo.mType=="cubemap" ) header += "uniform samplerCube iChannel" + i + ";\n";
        else if( inp.mInfo.mType=="volume"  ) header += "uniform sampler3D iChannel" + i + ";\n";
        else                                  header += "uniform sampler2D iChannel" + i + ";\n";

        // new API (see shadertoy.com/view/wtdGW8)
        header += "uniform struct {\n";
             if( inp==null )                  header += "  sampler2D";
        else if( inp.mInfo.mType=="cubemap" ) header += "  samplerCube";
        else if( inp.mInfo.mType=="volume"  ) header += "  sampler3D";
        else                                  header += "  sampler2D";
        header +=        " sampler;\n";
        header += "  vec3  size;\n";
        header += "  float time;\n";
        header += "  int   loaded;\n";
        header += "}iCh" + i + ";\n";
    }
	header += "void mainImage( out vec4 c,  in vec2 f );\n";
    header += "void st_assert( bool cond );\n";
    header += "void st_assert( bool cond, int v );\n";

    mImagePassFooter = "";
    if( mIs20 ) 
    {
        mImagePassFooter += "\nout vec4 shadertoy_out_color;\n" +
        "void st_assert( bool cond, int v ) {if(!cond){if(v==0)shadertoy_out_color.x=-1.0;else if(v==1)shadertoy_out_color.y=-1.0;else if(v==2)shadertoy_out_color.z=-1.0;else shadertoy_out_color.w=-1.0;}}\n" +
        "void st_assert( bool cond        ) {if(!cond)shadertoy_out_color.x=-1.0;}\n" +
        "void main( void )" +
        "{" +
            "shadertoy_out_color = vec4(1.0,1.0,1.0,1.0);" + 
            "vec4 color = vec4(0.0,0.0,0.0,1.0);" +
            "mainImage( color, gl_FragCoord.xy );" +
            "if(shadertoy_out_color.x<0.0) color=vec4(1.0,0.0,0.0,1.0);" +
            "if(shadertoy_out_color.y<0.0) color=vec4(0.0,1.0,0.0,1.0);" +
            "if(shadertoy_out_color.z<0.0) color=vec4(0.0,0.0,1.0,1.0);" +
            "if(shadertoy_out_color.w<0.0) color=vec4(1.0,1.0,0.0,1.0);" +
            "shadertoy_out_color = vec4(color.xyz,1.0);" +
        "}";
    }
    else
    {
        mImagePassFooter += "" +
        "void st_assert( bool cond, int v ) {if(!cond){if(v==0)gl_FragColor.x=-1.0;else if(v==1)gl_FragColor.y=-1.0;else if(v==2)gl_FragColor.z=-1.0;else gl_FragColor.w=-1.0;}}\n" +
        "void st_assert( bool cond        ) {if(!cond)gl_FragColor.x=-1.0;}\n" +
        "void main( void )" +
        "{" +
            "gl_FragColor = vec4(0.0,0.0,0.0,1.0);" + 
            "vec4 color = vec4(0.0,0.0,0.0,1.0);" +
            "mainImage( color, gl_FragCoord.xy );" +
            "color.w = 1.0;" +
            "if(gl_FragColor.w<0.0) color=vec4(1.0,0.0,0.0,1.0);" +
            "if(gl_FragColor.x<0.0) color=vec4(1.0,0.0,0.0,1.0);" +
            "if(gl_FragColor.y<0.0) color=vec4(0.0,1.0,0.0,1.0);" +
            "if(gl_FragColor.z<0.0) color=vec4(0.0,0.0,1.0,1.0);" +
            "if(gl_FragColor.w<0.0) color=vec4(1.0,1.0,0.0,1.0);" +
            "gl_FragColor = vec4(color.xyz,1.0);"+
        "}";
    }

    mHeader = header;
    mHeaderLength = 0;
}

function Sampler2Renderer(sampler)
{
    var filter = mRenderer.FILTER.NONE;
    if (sampler.filter === "linear") filter = mRenderer.FILTER.LINEAR;
    if (sampler.filter === "mipmap") filter = mRenderer.FILTER.MIPMAP;
    var wrap = mRenderer.TEXWRP.REPEAT;
    if (sampler.wrap === "clamp") wrap = mRenderer.TEXWRP.CLAMP;
    var vflip = false;
    if (sampler.vflip === "true") vflip = true;

    return { mFilter: filter, mWrap: wrap, mVFlip: vflip };
}

function loadTextures(url) {
    var texture = {};
    texture.mInfo = url;
    texture.globject = null;
    texture.loaded = false;
    texture.image = new Image();
    texture.image.crossOrigin = '';
    texture.image.onload = function()
    {
        var rti = Sampler2Renderer(url.mSampler);
        texture.globject = mRenderer.CreateTextureFromImage(mRenderer.TEXTYPE.T2D, texture.image, mRenderer.TEXFMT.C4I8, rti.mFilter, rti.mWrap, rti.mVFlip);
        texture.loaded = true;
    }

    texture.image.src = url.mSrc;
    mInputs[0] = texture;
}