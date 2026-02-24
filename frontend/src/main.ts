import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

// Vant 组件
import {
  Button,
  Cell,
  CellGroup,
  NavBar,
  Tabbar,
  TabbarItem,
  Field,
  Form,
  Toast,
  Dialog,
  Popup,
  ActionSheet,
  SwipeCell,
  Card,
  Tag,
  Icon,
  List,
  PullRefresh,
  Empty,
  Search,
  Tabs,
  Tab,
  Grid,
  GridItem,
  Image,
  Uploader,
  DatePicker,
  TimePicker,
  Picker,
  Loading,
  Skeleton,
  NoticeBar,
  Badge,
  Sticky,
  Divider,
  Collapse,
  CollapseItem,
  Swipe,
  SwipeItem,
  Lazyload
} from 'vant'

// Vant 样式
import 'vant/lib/index.css'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

// 注册 Vant 组件
app.use(Button)
app.use(Cell)
app.use(CellGroup)
app.use(NavBar)
app.use(Tabbar)
app.use(TabbarItem)
app.use(Field)
app.use(Form)
app.use(Toast)
app.use(Dialog)
app.use(Popup)
app.use(ActionSheet)
app.use(SwipeCell)
app.use(Card)
app.use(Tag)
app.use(Icon)
app.use(List)
app.use(PullRefresh)
app.use(Empty)
app.use(Search)
app.use(Tabs)
app.use(Tab)
app.use(Grid)
app.use(GridItem)
app.use(Image)
app.use(Uploader)
app.use(DatePicker)
app.use(TimePicker)
app.use(Picker)
app.use(Loading)
app.use(Skeleton)
app.use(NoticeBar)
app.use(Badge)
app.use(Sticky)
app.use(Divider)
app.use(Collapse)
app.use(CollapseItem)
app.use(Swipe)
app.use(SwipeItem)
app.use(Lazyload)

app.mount('#app')
