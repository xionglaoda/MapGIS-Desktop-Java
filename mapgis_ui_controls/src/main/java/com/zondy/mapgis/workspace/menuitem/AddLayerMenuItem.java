package com.zondy.mapgis.workspace.menuitem;

import com.zondy.mapgis.base.MessageBox;
import com.zondy.mapgis.base.SortLayers;
import com.zondy.mapgis.base.Window;
import com.zondy.mapgis.common.MapLayerHelper;
import com.zondy.mapgis.controls.MapControl;
import com.zondy.mapgis.filedialog.GDBOpenFileDialog;
import com.zondy.mapgis.map.*;
import com.zondy.mapgis.workspace.StaticFunction;
import com.zondy.mapgis.workspace.engine.ISingleMenuItem;
import com.zondy.mapgis.workspace.engine.IWorkspace;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 添加图层
 *
 * @author cxy
 * @date 2019/11/04
 */
public class AddLayerMenuItem implements ISingleMenuItem {
    private IWorkspace workspace;

    /**
     * 点击菜单项
     *
     * @param item 文档项
     */
    @Override
    public void onClick(DocumentItem item) {
        GDBOpenFileDialog dialog = new GDBOpenFileDialog();
        String filter_XRas = String.format("%s、%s、%s|ras;rcat;mds|", "栅格数据集", "栅格目录", "镶嵌数据集");
        String filter_AllFile = "|sfclsp;sfclsl;sfclsr;acls;ocls;ncls;mapset;ras;rcat;mds;*.wt;*.wl;*.wp;*.msi;*.hdf;*.tdf|";

        String commonUseImgFilter = getCommonUseImgFilter();
        String imgFilter = getImgFilter();
        dialog.setFilter("MapGIS 所有文件" + filter_AllFile
                + "简单要素类" + "|sfclsp;sfclsl;sfclsr|"
                + "注记类" + "|acls|"
                + "对象类" + "|ocls|"
                + "网络类" + "|ncls|"
                + "地图集" + "|mapset|"
                + filter_XRas
                + String.format("%s(%s)|%s|", "常用影像文件", commonUseImgFilter, commonUseImgFilter)
                + String.format("%s(%s)|%s|", "所有影像文件", imgFilter, imgFilter)
                + String.format("%s(*.*)|*.*", "所有文件"));
        dialog.setMultiSelect(true);

        Optional<String[]> optional = dialog.showAndWait();
        if (optional != null && optional.isPresent()) {
            String[] fileNames = optional.get();
            if (fileNames.length > 0) {
                boolean refresh = false;
                this.workspace.beginUpdateTree();
                StringBuilder existed = new StringBuilder();
                ArrayList<MapLayer> existLayers = new ArrayList<>();
                ArrayList<DocumentItem> items = new ArrayList<>();
                boolean noLongerPrompt = false;
                for (String url : fileNames) {
                    boolean cancel = false;
                    MapLayer mapLayer = MapLayerHelper.createMapLayerByUrl(url, true/*, out isCancel*/);
                    if (mapLayer != null) {
                        if (mapLayer instanceof VectorLayer) {
                            VectorLayer vectorLayer = (VectorLayer) mapLayer;
                            vectorLayer.setIsSymbolic(true);
                            vectorLayer.setIsFollowZoom(false);
                        }
                        if (StaticFunction.isLayerExisted(mapLayer, item)) {
                            existed.append("\r\n    ").append(mapLayer.getURL());
                            existLayers.add(mapLayer);
                        } else {
                            refresh = true;
                            if (item instanceof Map) {
                                ((Map) item).append(mapLayer);
                            } else if (item instanceof GroupLayer) {
                                ((GroupLayer) item).append(mapLayer);
                            }
                            SortLayers.sortTargetLayer(mapLayer);
                        }
                    } else {
                        if (cancel) {
                            continue;
                        }
                        if (!noLongerPrompt) {
//                    if (gdbShell.FileNames.Length > 1) {
//                        MapGIS.UI.Controls.MapGISErrorForm.ShowLastErrorEx(string.Format("{0}\"{1}\"{2}.", Resources.String_Open, url, Resources.String_Failed), out noLongerPrompt);
//                    } else {
//                        MapGIS.UI.Controls.MapGISErrorForm.ShowLastError();
//                    }
                        }
                    }
                }
                this.workspace.endUpdateTree();
                if (!existed.toString().equals("")) {
                    if (ButtonType.YES == MessageBox.questionEx(String.format("地图中已包含下列数据:%s是否要重复添加？", existed + "\r\n\r\n"), Window.primaryStage, false, "提示")) {
                        refresh = true;
                        for (MapLayer layer : existLayers) {
                            if (item instanceof Map) {
                                ((Map) item).append(layer);
                            } else if (item instanceof GroupLayer) {
                                ((GroupLayer) item).append(layer);
                            }
                            SortLayers.sortTargetLayer(layer);
                        }
                    } else {
                        for (MapLayer layer : existLayers) {
                            layer.dispose();
                        }
                    }
                }

                Map ownMap = StaticFunction.getOwnerMap(item);
                MapControl mc = this.workspace.getMapControl(ownMap);
                if (mc != null && refresh/* && ViewInfoHelp.GetSoonFresh(mc)*/) {
                    mc.refreshWnd();
                }
            }
        }
    }

    /**
     * 获取菜单项图标
     *
     * @return 菜单项图标
     */
    @Override
    public Image getImage() {
        return new Image(getClass().getResourceAsStream("/Png_AddLayer_16.png"));
    }

    /**
     * 获取命令按钮的标题
     *
     * @return 标题
     */
    @Override
    public String getCaption() {
        return "添加图层";
    }

    /**
     * 获取命令按钮是否可用
     *
     * @return true/false
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 获取命令按钮是否可见
     *
     * @return true/false
     */
    @Override
    public boolean isVisible() {
        return true;
    }

    /**
     * 获取命令按钮是否选中
     *
     * @return true/false
     */
    @Override
    public boolean isChecked() {
        return false;
    }

    /**
     * 获取是否启用分割符
     *
     * @return true/false
     */
    @Override
    public boolean isBeginGroup() {
        return true;
    }

    /**
     * 创建后事件
     *
     * @param ws 工作空间引擎
     */
    @Override
    public void onCreate(IWorkspace ws) {
        workspace = ws;
    }

    /**
     * 获取带扩展名的所有影像文件过滤符(例如返回结果：*.msi;*.img;*.tif;*.grd)
     *
     * @return
     */
    private String getImgFilter() {
        String rtn2 = "";
//        RasFileExtInfo[] infos = RasterDataSet.GetRasFileExtInfo(RasFileExtType.Input);
//        if (infos != null && infos.Length > 0) {
//            for(RasFileExtInfo info : infos)
//            {
//                if (!info.Descripe.Contains("*.*")) {
//                    if (info.FileExt1 != null && info.FileExt1.Trim().Length > 0) {
//                        rtn2 += "*." + info.FileExt1 + ";";
//                    }
//                    if (info.FileExt2 != null && info.FileExt2.Trim().Length > 0) {
//                        rtn2 += "*." + info.FileExt2 + ";";
//                    }
//                }
//            }
//        }
        return rtn2 != "" ? rtn2 : getCommonUseImgFilter();
    }

    private String getCommonUseImgFilter() {
        return "*.msi;*.img;*.tif;*.png;*.jpg;*.bmp;*.gif;*.jp2";
    }
}
